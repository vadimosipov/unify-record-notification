package ru.cti.iss.verint.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.cti.iss.verint.dao.VerintDao;
import ru.cti.iss.verint.model.Channel;
import ru.cti.iss.verint.model.RecordType;

import java.util.List;
import java.util.stream.Collectors;

public class VerintService {
    private static final Logger logger = LoggerFactory.getLogger(VerintService.class);

    private VerintDao verintDao;
    private VerintNotifierService notifierService;
    private VerintMonitoringService monitoringService;

    public VerintService(VerintDao verintDao, VerintNotifierService verintClientActionService, VerintMonitoringService monitoringService) {
        this.verintDao = verintDao;
        this.notifierService = verintClientActionService;
        this.monitoringService = monitoringService;
    }

    public List<Channel> findAll() {
        return verintDao.findAll()
                .stream()
                .filter(channel -> {
                    if (!channel.getType().isStartOnTrigger()) {
                        logger.warn("Found a phone number: {} with type: {} was skipped", channel.getExtension(), channel.getType());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public void requestChannelForPersist(final String extension) {
        final Channel channel = getChannelByExtension(extension);
        if (channel != null) {
            logger.info("Requesting Verint server to persist channel for device {}...", extension);
            try {
                notifierService.requestPersisting(extension);

                if (!channel.isStartOnTriggerType()) {
                    logger.error("Extension {} has incorrect type to be persisted. Should be: {}, current value: {}", extension, RecordType.START_ON_TRIGGER, channel.getType());
                    monitoringService.registerBadExtension(channel.getExtension());
                }
            } catch (Exception e) {
                logger.error("Error get send request to verint for extension {}", extension);
                monitoringService.increaseVerintDbError();
                monitoringService.increaseNotificationFailedError();
            }
        }
    }

    private Channel getChannelByExtension(final String extension) {
        try {
            final List<Channel> channelsByExtension = verintDao.findByExtension(extension);

            if (channelsByExtension.size() > 1) {
                logger.warn("Found {} channels for extension {}. Using first record.", channelsByExtension.size(), extension);
            }

            if (channelsByExtension.size() >= 1) {
                return channelsByExtension.get(0);
            }

            logger.error("Cannot find record for extension {} in verint database", extension);
        } catch (final Exception e) {
            logger.error("Load agent extension {} from Verint database fails", extension, e);
        }
        monitoringService.increaseVerintDbError();
        return null;
    }
}
