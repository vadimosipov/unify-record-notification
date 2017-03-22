package ru.cti.sipphone.pool.phone;

public class PhoneSettings {

    public final String number;
    public final int port;

    public PhoneSettings(String number, int port) {
        this.number = number;
        this.port = port;
    }
}
