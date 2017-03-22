package ru.cti.iss.verint.service;

public interface VerintMonitoringService {
    void increaseVerintDbError();
    void increaseVerintWsError();
    void increaseNotificationFailedError();
    void registerBadExtension(String phoneNumber);
}
