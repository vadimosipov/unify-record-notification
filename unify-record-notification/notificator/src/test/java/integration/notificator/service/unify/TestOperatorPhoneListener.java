package integration.notificator.service.unify;

import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import ru.cti.sipphone.pool.phone.PhoneListener;

public class TestOperatorPhoneListener extends PhoneListener {

    public TestOperatorPhoneListener(TestOperatorPhone phone) {
        super(phone);
    }

    @Override
    public void incomingCall(SipRequest sipRequest, SipResponse response) {
        super.incomingCall(sipRequest, response);
        getTestPhone().answerCall(sipRequest);
        getTestPhone().startNotification(sipRequest);
    }

    private TestOperatorPhone getTestPhone() {
        return (TestOperatorPhone) phone;
    }
}
