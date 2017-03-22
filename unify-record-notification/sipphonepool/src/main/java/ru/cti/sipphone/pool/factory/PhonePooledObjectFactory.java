package ru.cti.sipphone.pool.factory;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.cti.sipphone.pool.phone.GeneralSettings;
import ru.cti.sipphone.pool.phone.Phone;
import ru.cti.sipphone.pool.phone.PhoneListener;
import ru.cti.sipphone.pool.phone.PhoneSettings;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class PhonePooledObjectFactory extends BasePooledObjectFactory<Phone> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final long MAX_TIME_FOR_REGISTRATION_IN_MS = 1_000;

    private final Queue<PhoneSettings> spareSettings;
    private final GeneralSettings settings;

    public PhonePooledObjectFactory(Collection<PhoneSettings> initialSettings, GeneralSettings settings) {
        this.settings = settings;
        this.spareSettings = new ConcurrentLinkedQueue<>(initialSettings);
    }

    @Override
    public Phone create() throws Exception {
        final long timeLimit = currentTimeMillis() + MAX_TIME_FOR_REGISTRATION_IN_MS;
        PhoneSettings phoneSettings = spareSettings.poll();
        if (phoneSettings == null) {
            logger.error("No free phones for registering");
            throw new RuntimeException("No free phones for registering");
        }

        CountDownLatch latch = new CountDownLatch(1);
        final Phone phone = new Phone(phoneSettings, settings);
        phone.register(new PhoneListener(phone), p -> latch.countDown(), () -> destroyObject(wrap(phone)));

        if (latch.await(timeLimit - currentTimeMillis(), MILLISECONDS)) {
            return phone;
        }
        throw new RuntimeException("It failed to initialize a new phone on time. The time was exceed");
    }

    @Override
    public PooledObject<Phone> wrap(Phone obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(final PooledObject<Phone> po) {
        Phone phone = po.getObject();
        logger.info("Turn off the phone: {}", phone.getNumber());
        phone.close();
        spareSettings.add(phone.getSettings());
    }
}
