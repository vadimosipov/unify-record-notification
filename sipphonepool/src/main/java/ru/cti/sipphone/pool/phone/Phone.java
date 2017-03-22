package ru.cti.sipphone.pool.phone;

import net.sourceforge.peers.FileLogger;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.javaxsound.JavaxSoundManager;
import net.sourceforge.peers.media.AbstractSoundManager;
import net.sourceforge.peers.sip.core.useragent.UserAgent;
import net.sourceforge.peers.sip.syntaxencoding.SipUriSyntaxException;
import net.sourceforge.peers.sip.transport.SipRequest;

import java.io.Closeable;
import java.net.SocketException;
import java.util.function.Consumer;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

public class Phone implements Closeable {

    protected final org.slf4j.Logger logger = getLogger(getClass());

    private final String sipFormat = "sip:%s@%s";
    private SipRequest callRequest;
    private PhoneListener listener;

    protected volatile UserAgent userAgent;
    private volatile boolean registered;

    private final PhoneConfig config;

    public Phone(PhoneSettings settings, GeneralSettings generalSettings) {
        config = new PhoneConfig(settings, generalSettings);
    }

    public void register(PhoneListener listener, Consumer<Phone> onSuccess, Runnable onFailed) {
        initialize(listener);
        listener.onRegistration(new CallHandler(onSuccess, onFailed));
        try {
            logger.info("Phone: {}, state: trying to register, sip port: {}", getNumber(), userAgent.getSipPort());
            userAgent.register();
        } catch (SipUriSyntaxException e) {
            throw new RuntimeException(format("Phone: %s, state: an error caused during registering of the user agent", getNumber()), e);
        }
    }

    private void initialize(PhoneListener listener) {
        final Logger logger = new FileLogger(null);
        final AbstractSoundManager soundManager = new JavaxSoundManager(false, /*TODO config.isMediaDebug(),*/ logger, null);
        this.listener = listener;
        try {
            userAgent = new UserAgent(listener, config, logger, soundManager);
        } catch (SocketException e) {
            throw new RuntimeException(format("Phone: %s, state: an error happened during initialization of the user agent", getNumber()));
        }
    }

    public void call(String number, Runnable errorHandler) {
        listener.onFailedCall(errorHandler);
        final String callee = format(sipFormat, number, config.getDomain());
        logger.info("Phone: {}, state: trying to make a call to {}.", getNumber(), callee);
        try {
            callRequest = userAgent.invite(callee, null);
        } catch (SipUriSyntaxException e) {
            logger.error(format("Phone: %s, state: failed to make a call due to the wrong phone number format (%s)", getNumber(), callee), e);
        }
    }

    public void hangup() {
        logger.info("Phone: {}, state: trying to hang up the call", getNumber());
        if (callRequest != null) {
            userAgent.terminate(callRequest);
            callRequest = null;
        }
        logger.info("Phone: {}, state: hung up", getNumber());
    }

    private void unregister() {
        registered = false;
        logger.info("Phone: {}, state: unregistering", getNumber());
        try {
            userAgent.unregister();
            logger.info("Phone: {}, state: unregistered", getNumber());
        } catch (Exception e) {
            logger.error("Failed to unregister for the phone: {}", getNumber(), e);
        }
    }

    @Override
    public void close() {
        unregister();
        logger.info("Phone: {}, state: turning off", getNumber());
        userAgent.close();
        logger.info("Phone: {}, state: turned off", getNumber());
    }

    public String getNumber() {
        return config.getUserPart();
    }

    public PhoneSettings getSettings() {
        return config.getPhoneSettings();
    }

    void setRegistered() {
        registered = true;
    }

    public boolean isRegistered() {
        return registered;
    }
}
