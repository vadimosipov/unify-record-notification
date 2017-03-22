package integration.notificator.service.unify;

import integration.notificator.TestNotificationConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.cti.iss.unify.notificator.service.csta.CstaService;

import javax.inject.Inject;
import java.util.Set;

import static org.junit.Assert.assertEquals;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestNotificationConfiguration.class)
@ActiveProfiles("test")
public class CstaServiceTest {

    private final String operatorNumber = "+74952001002";

    @Inject
    private CstaService cstaService;

    @Test
    public void restart() {
        cstaService.restart();
    }

    @Test
    public void shouldMonitorDevice() {
        try {
            cstaService.startDeviceMonitoring(operatorNumber);
            Set<String> monitoredPhones = cstaService.getMonitoredPhones();
            assertEquals(1, monitoredPhones.size());
            assertEquals(operatorNumber, monitoredPhones.iterator().next());
        } finally {
            cstaService.stopDeviceMonitoring(operatorNumber);
            Set<String> monitoredPhones = cstaService.getMonitoredPhones();
            assertEquals(0, monitoredPhones.size());
        }
    }
}
