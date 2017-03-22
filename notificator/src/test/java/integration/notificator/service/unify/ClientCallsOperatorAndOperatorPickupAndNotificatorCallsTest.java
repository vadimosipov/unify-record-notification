package integration.notificator.service.unify;

import com.sen.openscape.csta.provider.CstaEventListener;
import integration.notificator.TestNotificationConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.cti.iss.unify.notificator.service.PhoneService;
import ru.cti.iss.unify.notificator.service.csta.CstaDeviceData;
import ru.cti.iss.unify.notificator.service.csta.CstaService;
import ru.cti.sipphone.pool.phone.Phone;
import ru.cti.sipphone.pool.phone.PhoneListener;
import ru.cti.sipphone.pool.phone.PhoneSettings;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sen.openscape.csta.provider.CstaEventType.CONFERENCED;
import static com.sen.openscape.csta.provider.CstaEventType.ESTABLISHED;
import static com.sen.openscape.csta.util.CstaCause.activeParticipation;
import static com.sen.openscape.csta.util.CstaCause.newCall;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestNotificationConfiguration.class)
public class ClientCallsOperatorAndOperatorPickupAndNotificatorCallsTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String OPERATOR_NUMBER = "+7(495)2001003";
    private final String CLIENT_NUMBER = "74952001005";

    private final long TALK_DURATION = 60_000;
    private final long REGISTRATION_TIME = 20_000;

    private final CountDownLatch turn = new CountDownLatch(1);
    private final CountDownLatch globalLock = new CountDownLatch(1);
    private ExecutorService pool = new ForkJoinPool(2);

    private volatile boolean established = false;
    private volatile String conferenceCallID;

    @Inject
    private CstaService cstaService;

    @Inject
    private PhoneService phoneService;

    @Inject
    private TestNotificationConfiguration conf;

    @Test
    public void test() throws InterruptedException {
        cstaService.connect(createListener());
        cstaService.startDeviceMonitoring(OPERATOR_NUMBER);

        CompletableFuture of = runAsync(this::startOperator, pool);
        CompletableFuture oc = runAsync(this::startClient, pool);

        logger.info("-----------------------TALK_DURATION-----------------------");
        Thread.sleep(TALK_DURATION);
        globalLock.countDown();

        try {
            of.get();
            oc.get();
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            cstaService.stopDeviceMonitoring(OPERATOR_NUMBER);
        }

        assertTrue("Failed to move to " + ESTABLISHED + " phase", established);
        assertNotNull("Failed to move to " + CONFERENCED + " phase", conferenceCallID);


        phoneService.talkAndHangup(OPERATOR_NUMBER, conferenceCallID);
    }

    private void startOperator() {
        PhoneSettings settings = new PhoneSettings(OPERATOR_NUMBER, 0);
        try (TestOperatorPhone phone = new TestOperatorPhone(settings, conf.generalSettings())) {

            final CountDownLatch regLatch = new CountDownLatch(1);
            final AtomicBoolean success = new AtomicBoolean(false);
            phone.register(new TestOperatorPhoneListener(phone),
                    p -> { regLatch.countDown(); success.set(true); turn.countDown(); },
                    () -> { regLatch.countDown(); success.set(false); });
            assertTrue("Time is over. The client was not registered on time", regLatch.await(REGISTRATION_TIME, MILLISECONDS));
            assertTrue("Failed to register the client phone", success.get());

            globalLock.await();
        } catch (InterruptedException e) {
            logger.error("Some exception happened", e);
        }
    }

    private void startClient() {
        PhoneSettings settings = new PhoneSettings(CLIENT_NUMBER, 0);
        try (Phone phone = new Phone(settings, conf.generalSettings())) {

            final CountDownLatch regLatch = new CountDownLatch(1);
            final AtomicBoolean success = new AtomicBoolean(false);
            phone.register(new PhoneListener(phone),
                    p -> { regLatch.countDown(); success.set(true); },
                    () -> { regLatch.countDown(); success.set(false); });

            assertTrue("Time is over. The client was not registered on time", regLatch.await(REGISTRATION_TIME, MILLISECONDS));
            assertTrue("Failed to register the client phone", success.get());
            assertTrue("Time is over. The operator was not registered on time", turn.await(REGISTRATION_TIME, MILLISECONDS));

            phone.call(OPERATOR_NUMBER, () -> { success.set(false); globalLock.countDown(); });

            globalLock.await();
            assertTrue("Failed to make a call to the operator", success.get());
        } catch (InterruptedException e) {
            logger.error("Some exception happened", e);
        }
    }

    private CstaEventListener createListener() {
        return event -> {
            String fqnDn = event.fqnDn;
            logger.info("CSTA Event type: {}, phone: {}, callID: {}", event.evtType, fqnDn, event.callID);
            switch (event.evtType) {
                case ESTABLISHED:
                    if (event.evtConnection.cause == newCall) {
                        CstaDeviceData monitoringData = cstaService.getData(fqnDn);
                        if (monitoringData == null) {
                            fail("Got an event, but there is no monitored data about the listened operator");
                        }
                        phoneService.call(fqnDn, event.callID);
                        established = true;
                    }
                    break;
                case CONFERENCED:
                    CstaDeviceData monitoringData = cstaService.getData(fqnDn);
                    if (monitoringData == null) {
                        fail("Got an event, but there is no monitored data about the listened operator");
                    }
                    Assert.assertEquals(OPERATOR_NUMBER, fqnDn);
                    conferenceCallID = event.callID;
                    break;
            }
        };
    }
}
