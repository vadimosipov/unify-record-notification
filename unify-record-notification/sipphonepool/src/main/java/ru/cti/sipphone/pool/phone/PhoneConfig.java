package ru.cti.sipphone.pool.phone;


import net.sourceforge.peers.Config;
import net.sourceforge.peers.media.MediaMode;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;

import java.net.InetAddress;

public class PhoneConfig implements Config {

    private final GeneralSettings settings;
    private final PhoneSettings phoneSettings;

    public PhoneConfig(PhoneSettings phoneSettings, GeneralSettings settings) {
        this.phoneSettings = phoneSettings;
        this.settings = settings;
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InetAddress getLocalInetAddress() {
        return settings.localAddress;
    }

    @Override
    public InetAddress getPublicInetAddress() {
        return null;
    }

    @Override
    public String getUserPart() {
        return phoneSettings.number;
    }

    @Override
    public String getDomain() {
        return settings.serverAddress;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public SipURI getOutboundProxy() {
        return null;
    }

    @Override
    public int getSipPort() {
        return phoneSettings.port;
    }

    @Override
    public MediaMode getMediaMode() {
        return MediaMode.file;
    }

    @Override
    public boolean isMediaDebug() {
        return false;
    }

    @Override
    public String getMediaFile() {
        return settings.mediaFileURL.getPath();
    }

    @Override
    public int getRtpPort() {
        return 0;
    }

    @Override
    public String getAuthorizationUsername() {
        return null;
    }

    @Override
    public void setLocalInetAddress(InetAddress inetAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPublicInetAddress(InetAddress inetAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserPart(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDomain(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPassword(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOutboundProxy(SipURI sipURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSipPort(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMediaMode(MediaMode mediaMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMediaDebug(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMediaFile(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRtpPort(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAuthorizationUsername(String s) {
        throw new UnsupportedOperationException();
    }

    public GeneralSettings getSettings() {
        return settings;
    }

    public PhoneSettings getPhoneSettings() {
        return phoneSettings;
    }
}
