package ru.cti.iss.unify.notificator.dto;

import ru.cti.sipphone.pool.phone.Phone;

import java.time.LocalTime;
import java.util.concurrent.Future;

public class Call {
    public final LocalTime startTime;
    public final Phone notificator;
    public volatile Future<?> futureHangup;
    public String conferenceCallID;

    public Call(Phone phone) {
        this.startTime = LocalTime.now();
        this.notificator = phone;
    }
}
