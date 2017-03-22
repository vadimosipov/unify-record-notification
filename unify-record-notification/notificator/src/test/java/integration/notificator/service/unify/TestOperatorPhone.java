package integration.notificator.service.unify;

import net.sourceforge.peers.FileLogger;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sdp.*;
import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaders;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipRequest;
import ru.cti.sipphone.pool.phone.GeneralSettings;
import ru.cti.sipphone.pool.phone.Phone;
import ru.cti.sipphone.pool.phone.PhoneSettings;

public class TestOperatorPhone extends Phone {

    public TestOperatorPhone(PhoneSettings settings, GeneralSettings generalSettings) {
        super(settings, generalSettings);
    }

    public void answerCall(SipRequest sipRequest) {
        logger.info("Phone {}: Answer call", getNumber());
        String callId = Utils.getMessageCallId(sipRequest);
        DialogManager dialogManager = userAgent.getDialogManager();
        Dialog dialog = dialogManager.getDialog(callId);
        userAgent.acceptCall(sipRequest, dialog);
    }

    public void startNotification(SipRequest sipRequest) {
        logger.info("Phone: {}, state: start notification", getNumber());

        SipHeaders reqHeaders = sipRequest.getSipHeaders();
        SipHeaderFieldValue contentType = reqHeaders.get(new SipHeaderFieldName(RFC3261.HDR_CONTENT_TYPE));
        byte[] offerBytes = sipRequest.getBody();

        MediaDestination mediaDestination;
        if (offerBytes != null && contentType != null && RFC3261.CONTENT_TYPE_SDP.equals(contentType.getValue())) {
            // create response in 200
            try {
                Logger logger = new FileLogger(null);
                SDPManager sdpManager = new SDPManager(userAgent, logger);
                SessionDescription answer = sdpManager.parse(offerBytes);
                mediaDestination = sdpManager.getMediaDestination(answer);

                String destAddress = mediaDestination.getDestination();
                int destPort = mediaDestination.getPort();
                Codec codec = mediaDestination.getCodec();

                userAgent.getMediaManager().handleAck(destAddress, destPort, codec);
            } catch (NoCodecException e) {
                throw new RuntimeException("No codec.", e);
            }
        } else {
            logger.error("Undefined content type or specification");
        }
    }
}
