package ru.cti.iss.unify.notificator.service.csta;

import com.sen.openscape.csta.provider.CstaEventListener;
import com.sen.openscape.csta.provider.CstaEventObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.cti.iss.unify.notificator.service.PhoneService;

import java.util.function.BiConsumer;

import static com.sen.openscape.csta.provider.CstaEventIndicator.ConnectionNumberChanged;
import static com.sen.openscape.csta.util.CstaCause.activeParticipation;
import static com.sen.openscape.csta.util.CstaCause.newCall;

public class CstaListener implements CstaEventListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CstaService cstaService;
    private final PhoneService phoneService;

    public CstaListener(CstaService cstaService, PhoneService phoneService) {
        this.cstaService = cstaService;
        this.phoneService = phoneService;
    }

    @Override
    public void newCstaEvent(CstaEventObject event) {
        logger.info("Event: csta, type: {}, operator: {}, callID: {}", event.evtType, event.fqnDn, event.callID);
        switch (event.evtType) {
            case ESTABLISHED:
                switch (event.evtConnection.cause) {
                    case newCall: handle(event, phoneService::call); break;
                    case activeParticipation: handle(event, phoneService::talkAndHangup); break;
                }
            case CONFERENCED:
                phoneService.inviteToConference(event.callID);
                break;
            case CONNECTIONCLEARED:
                if (event.evtInd == ConnectionNumberChanged) handle(event, phoneService::hangupNow);
                break;
            case CSTAEXCEPTION:
                /* TODO сделать обработку ошибки. Она может проявляться если что то пошло не так с соединением (закрыть порт или типо того)
                cstaService.restart()
                 */
                cstaService.restart();
                break;
        }
    }

    private void handle(CstaEventObject event, BiConsumer<String, String> method) {
        CstaDeviceData monitoringData = cstaService.getData(event.fqnDn);
        if (monitoringData != null) {
            method.accept(monitoringData.device.fqnDn, event.callID);
        }
    }
}
