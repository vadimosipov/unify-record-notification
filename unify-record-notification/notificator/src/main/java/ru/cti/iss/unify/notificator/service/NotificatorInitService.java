package ru.cti.iss.unify.notificator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import ru.cti.iss.unify.notificator.service.csta.CstaService;
import ru.cti.iss.verint.model.Channel;
import ru.cti.iss.verint.service.VerintService;

import java.util.Set;

public class NotificatorInitService {

    private Logger logger = LoggerFactory.getLogger(NotificatorInitService.class);

    private final CstaService cstaService;
    private final VerintService verintService;

    public NotificatorInitService(CstaService cstaService, VerintService verintService) {
        this.cstaService = cstaService;
        this.verintService = verintService;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 1_000)
    public void updateMonitoringNumbers() {
        logger.info("Update CSTA phone monitoring list");

        final Set<String> monitoredPhones = cstaService.getMonitoredPhones();
        verintService.findAll()
                .stream()
                .map(Channel::getExtension)
                .filter(extension -> !monitoredPhones.remove(extension))
                .forEach(cstaService::startDeviceMonitoring);

        monitoredPhones.forEach(cstaService::stopDeviceMonitoring);
    }
}


