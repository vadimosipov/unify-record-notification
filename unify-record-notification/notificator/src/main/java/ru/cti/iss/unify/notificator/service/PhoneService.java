package ru.cti.iss.unify.notificator.service;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import ru.cti.iss.unify.notificator.dto.Call;
import ru.cti.iss.verint.service.VerintService;
import ru.cti.sipphone.pool.phone.Phone;

import java.io.Closeable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

public class PhoneService implements Closeable {

    public final Logger logger = getLogger(getClass());
    private final Map<String, Call> callBook = new ConcurrentHashMap<>();
    private final String specialCode = "*73+";
    private final GenericObjectPool<Phone> phonePool;
    private final int durationInSec;
    private final WorkerService workerService;
    private final VerintService verintService;

    public PhoneService(int durationInSec, GenericObjectPool<Phone> phonePool, VerintService verintService, WorkerService workerService) {
        this.durationInSec = durationInSec;
        this.phonePool = phonePool;
        this.verintService = verintService;
        this.workerService = workerService;
    }

    public void call(String operatorNumber, String callID) {
        workerService.execute(() -> {
            logger.info("phone: ?, state: find a phone to make a call, operator: {}, callID: {}", operatorNumber, callID);
            final Phone phone = findPhone();
            if (phone != null) {
                logger.info("phone: {}, state: calling to the operator, operator: {}", phone.getNumber(), operatorNumber);
                call(operatorNumber, callID, phone);
            } else {
                logger.warn("phone: ?, state: skip the call, operator: {}, callID: {}", operatorNumber, callID);
            }
        });
    }

    private void call(String operatorNumber, String callID, Phone phone) {
        callBook.put(callID, new Call(phone));
        phone.call(specialCode + operatorNumber, () -> {
            logger.error("phone: {}, state: failed to make a call to the operator, operator: {}", phone.getNumber(), operatorNumber);
            callBook.remove(callID);
            try {
                phonePool.invalidateObject(phone);
            } catch (Exception e) {
                logger.error("phone: {}, state: failed to turn off the phone", phone.getNumber());
            }
        });
    }

    private Phone findPhone() {
        Phone phone = null;
        while (phone == null) {
            try {
                phone = phonePool.borrowObject();
            } catch (Exception e) {
                logger.error("Failed to borrow a phone from the phone pool", e);
                break;
            }
        }
        return phone;
    }

    public void inviteToConference(String callID) {
        Call call = callBook.get(callID);
        if (call != null) {
            call.conferenceCallID = callID;
        }
    }

    public void talkAndHangup(String operatorNumber, String callID) {
        final Call call = callBook.get(callID);
        if (call != null && callID.equals(call.conferenceCallID)) {
            logger.info("phone: {}, state: scheduling the hangup in {} seconds, operator: {}, callID: {}",
                    call.notificator.getNumber(), durationInSec, operatorNumber, callID);
            call.futureHangup = workerService.execute(() -> hangup(operatorNumber, callID, true), durationInSec);
        }
    }

    public void hangupNow(String operatorNumber, String callID) {
        Call call = callBook.get(callID);
        if (call != null && !call.futureHangup.isDone()) {
            logger.info("phone: {}, state: hangup the call, operator: {}, callID: {}", call.notificator.getNumber(), operatorNumber, callID);
            boolean notCompleted = call.futureHangup.cancel(false);
            if (notCompleted && callBook.get(callID) != null) {
                workerService.execute(() -> hangup(operatorNumber, callID, false));
            }
        }
    }

    private Runnable hangup(String operatorNumber, String callID, boolean record) {
        return () -> {
            Phone phone = callBook.remove(callID).notificator;
            phone.hangup();
            phonePool.returnObject(phone);
            if (record) {
                logger.info("phone: {}, state: send a message to notification service, operator: {}, callID: {}", phone.getNumber(), operatorNumber, callID);
                verintService.requestChannelForPersist(operatorNumber);
            }
        };
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 30_000)
    public void findAndDeleteOrphanedPhones() {
        Iterator<Map.Entry<String, Call>> it = callBook.entrySet().iterator();
        it.forEachRemaining(e -> {
            if (e.getValue()
                 .startTime
                 .plusSeconds(durationInSec)
                 .plusMinutes(1)
                 .isBefore(LocalTime.now())) {
                it.remove();
            }
        });
    }

    @Override
    public void close() {
        if (!callBook.isEmpty()) {
            Collection<Call> calls = new ArrayList<>(callBook.values());
            callBook.clear();
            for (Call call : calls) {
                // TODO возможно, нужна блокировка, пока копируем, телефоны могли уже освободиться корректно
                if (call.futureHangup == null && call.notificator.isRegistered()) {
                    phonePool.returnObject(call.notificator);
                }
            }
        }
    }
}
