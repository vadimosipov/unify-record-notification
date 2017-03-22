package ru.cti.sipphone.pool.phone;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class GeneralSettings {

    public final URL mediaFileURL;
    public final InetAddress localAddress;
    public final String serverAddress;

    public GeneralSettings(URL mediaFileURL, String localAddress, String serverAddress) {
        this.mediaFileURL = mediaFileURL;
        this.serverAddress = serverAddress;

        try {
            this.localAddress = InetAddress.getByName(localAddress);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error when finding local network interface. Please check configs.", e);
        }
    }
}
