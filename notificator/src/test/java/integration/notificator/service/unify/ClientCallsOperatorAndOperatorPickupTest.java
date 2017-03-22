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
import ru.cti.iss.unify.notificator.service.csta.CstaDeviceData;
import ru.cti.iss.unify.notificator.service.csta.CstaService;
import ru.cti.sipphone.pool.phone.Phone;
import ru.cti.sipphone.pool.phone.PhoneListener;
import ru.cti.sipphone.pool.phone.PhoneSettings;

import javax.inject.Inject;
import java.util.concurrent.*;

import static com.sen.openscape.csta.util.CstaCause.newCall;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestNotificationConfiguration.class)
public class ClientCallsOperatorAndOperatorPickupTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String OPERATOR_NUMBER = "74952001003";
    private final String CLIENT_NUMBER = "74952001005";
    private volatile boolean waitOperator = true;
    private final CountDownLatch turn = new CountDownLatch(1);
    private final long TALK_DURATION = 40_000;

    @Inject
    private CstaService cstaService;

    @Inject
    private TestNotificationConfiguration conf;

    @Test
    public void test() throws InterruptedException {
        cstaService.connect(createListener());
        cstaService.startDeviceMonitoring(OPERATOR_NUMBER);
        ExecutorService pool = new ForkJoinPool(2);
        try {
            runAsync(pool, this::startOperator);
            runAsync(pool, this::startClient);
            Thread.sleep(TALK_DURATION);
        } finally {
            cstaService.stopDeviceMonitoring(OPERATOR_NUMBER);
        }
    }

    private void runAsync(ExecutorService service, Startable startable) {
        CompletableFuture.runAsync(() -> {
            try {
                startable.start();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, service);
    }

    private void startOperator() throws InterruptedException {
        PhoneSettings settings = new PhoneSettings(OPERATOR_NUMBER, 0);
        try (TestOperatorPhone phone = new TestOperatorPhone(settings, conf.generalSettings())) {

            CountDownLatch latch = new CountDownLatch(1);
            CompletableFuture.runAsync(() ->
                    phone.register(
                            new TestOperatorPhoneListener(phone),
                            p -> latch.countDown(),
                            () -> fail("Failed to register the operator phone")
                    ));
            Assert.assertTrue("Time is over. The operator was not registered on time", latch.await(5, TimeUnit.SECONDS));
            waitOperator = false;
            turn.countDown();

            Thread.sleep(TALK_DURATION);
        }
    }

    private void startClient() throws InterruptedException {
        PhoneSettings settings = new PhoneSettings(CLIENT_NUMBER, 0);
        try (Phone phone = new Phone(settings, conf.generalSettings())) {

            CountDownLatch latch = new CountDownLatch(1);
            CompletableFuture.runAsync(() ->
                    phone.register(
                            new PhoneListener(phone),
                            p -> latch.countDown(),
                            () -> fail("Failed to register the operator phone"))
            );
            Assert.assertTrue("Time is over. The operator was not registered on time", latch.await(5, TimeUnit.SECONDS));

            if (waitOperator) {
                turn.await(5, TimeUnit.SECONDS);
            }
            if (waitOperator) {
                Assert.fail("Time is over. The operator was not registered on time");
            }

            phone.call(OPERATOR_NUMBER, () -> fail("Failed to call the operator"));

            Thread.sleep(TALK_DURATION);
        }
    }

    private CstaEventListener createListener() {
        return event -> {
            logger.info("CSTA Event type: {}, phone: {}, callID: {}", event.evtType, event.fqnDn, event.callID);
            switch (event.evtType) {
                case ESTABLISHED:
                    if (event.evtConnection.cause == newCall) {
                        CstaDeviceData monitoringData = cstaService.getData(event.fqnDn);
                        if (monitoringData != null) {
                            Assert.assertEquals(OPERATOR_NUMBER, monitoringData.device.fqnDn);
                            return;
                        }
                        fail();
                    }
                    break;
            }
        };
    }

    @FunctionalInterface
    private interface Startable { void start() throws InterruptedException; }
}
