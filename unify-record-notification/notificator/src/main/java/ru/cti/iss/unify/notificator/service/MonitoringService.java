package ru.cti.iss.unify.notificator.service;

import ru.cti.iss.verint.service.VerintMonitoringService;

public class MonitoringService implements VerintMonitoringService {

    @Override
    public void increaseVerintDbError() {}

    @Override
    public void increaseVerintWsError() {}

    @Override
    public void increaseNotificationFailedError() {}

    @Override
    public void registerBadExtension(String phoneNumber) {}
}
