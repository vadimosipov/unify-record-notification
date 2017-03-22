package ru.cti.sipphone.pool.phone;

import net.sourceforge.peers.sip.core.useragent.SipListener;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import org.slf4j.Logger;

import java.util.function.Consumer;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

public class PhoneListener implements SipListener {

    private final Logger logger = getLogger(getClass());

    protected final Phone phone;
    private Runnable failedCallHandler;
    private CallHandler registrationHandler;

    public PhoneListener(Phone phone) {
        this.phone = phone;
    }

    @Override
    public void registering(SipRequest sipRequest) {
        print(logger::info, format("Event: sip, type: registering, phone: %s", phone.getNumber()), new SipResponse(-1, "None"));
    }

    @Override
    public void registerSuccessful(SipResponse response) {
        print(logger::info, format("Event: sip, type: registered, phone: %s", phone.getNumber()), response);
        phone.setRegistered();
        if (registrationHandler != null) {
            registrationHandler.onSuccess.accept(phone);
        }
    }

    @Override
    public void registerFailed(SipResponse response) {
        print(logger::error, format("Event: sip, type: registering failed, phone: %s", phone.getNumber()), response);
        if (registrationHandler != null) {
            registrationHandler.onFailed.run();
        }
    }

    @Override
    public void incomingCall(SipRequest sipRequest, SipResponse response) {
        print(logger::info, format("Event: sip, type: incoming call, phone: %s", phone.getNumber()), response);
    }

    @Override
    public void ringing(SipResponse response) {
        print(logger::info, format("Event: sip, type: ringing, phone: %s", phone.getNumber()), response);
    }

    @Override
    public void calleePickup(SipResponse response) {
        print(logger::info, format("Event: sip, type: callee picked up, phone: %s", phone.getNumber()), response);
    }

    @Override
    public void remoteHangup(SipRequest sipRequest) {
        print(logger::info, format("Event: sip, type: remote hungup, phone: %s", phone.getNumber()), new SipResponse(-1, "None"));
    }

    @Override
    public void error(SipResponse response) {
        print(logger::error, format("Event: sip, type: error, phone: %s", phone.getNumber()), response);
        if (failedCallHandler != null) {
            failedCallHandler.run();
            failedCallHandler = null;
        }
    }

    private void print(Consumer<String> log, String text, SipResponse response) {
        if (response != null) {
            String message = "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"
                    + text
                    + "\nStatus code: " + response.getStatusCode()
                    + "\nPhrase: " + response.getReasonPhrase()
                    + "\nHeaders: \n" + response.getSipHeaders()
                    + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<";
            log.accept(message);
        }
    }

    public void onFailedCall(Runnable handler) {
        this.failedCallHandler = handler;
    }

    public void onRegistration(CallHandler registrationHandler) {
        this.registrationHandler = registrationHandler;
    }
}