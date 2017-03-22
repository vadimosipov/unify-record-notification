package ru.cti.iss.unify.notificator.service.csta;

import com.sen.openscape.csta.callcontrol.CstaDevice;
import com.sen.openscape.csta.provider.CstaEventListener;
import com.sen.openscape.csta.provider.CstaProvider;
import com.sen.openscape.csta.util.CstaConfiguration;
import com.sen.openscape.csta.util.CstaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class CstaService implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, CstaDeviceData> monitoredPhones = new ConcurrentHashMap<>();
    private final Object SYNC = new Object();
    private final long connectTimeoutInSec = 10;

    private final String address;
    private final int port;

    private CstaProvider provider = new CstaProvider();
    private CstaEventListener listener;

    public CstaService(String serverIpAddress, int serverPort) {
        this.address = serverIpAddress;
        this.port = serverPort;
    }

    public void connect(CstaEventListener listener) {
        logger.info("Connecting to the CSTA server: {}:{}...", address, port);
        ForkJoinTask<?> task = ForkJoinPool.commonPool().submit(() -> {
            try {
                provider.connectToSystem(new CstaConfiguration(address, port));
            } catch (CstaException e) {
                logger.error(String.format("An error occurred during connecting to the CSTA server: %s:%s", address, port));
                throw new RuntimeException(e);
            }
            provider.registerEventListener(listener);
            provider.startHeartbeat(30, 30);
            this.listener = listener;
        });
        try {
            task.get(connectTimeoutInSec, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startDeviceMonitoring(String phoneNumber) {
        logger.info("Start the CSTA monitoring the phone: {}", phoneNumber);
        CstaDeviceData monitoringData = monitoredPhones.get(phoneNumber);
        if (monitoringData == null) {
            synchronized (SYNC) {
                CstaDevice device = provider.addDevice(phoneNumber);
                try {
                    String crossRefId = provider.MonitorStart(device).crossRefId;
                    monitoringData = new CstaDeviceData(device, crossRefId);
                    monitoredPhones.put(phoneNumber, monitoringData);
                } catch (CstaException e) {
                    logger.error(format("An error occurred during starting the CSTA monitoring on the phone: %s", phoneNumber), e);
                    provider.removeDevice(phoneNumber);
                }
                /*
                CstaFilterEventList filter = new CstaFilterEventList();
                filter.add(CstaFilterEvent.CSTA_FILTER_EVENT_ESTABLISHED);
                filter.add(CstaFilterEvent.CSTA_FILTER_EVENT_CONFERENCED);
                filter.add(CstaFilterEvent.CSTA_FILTER_EVENT_FAILED);
                //            CstaMonitor monitor = provider.MonitorStart(device, false, false, filter);
                 */
            }
        }
    }

    public void stopDeviceMonitoring(String phoneNumber) {
        CstaDeviceData monitoringData = monitoredPhones.remove(phoneNumber);
        if (monitoringData != null) {
            stopAndRemoveDevice(phoneNumber, monitoringData.crossRefId);
        }
    }

    private void stopAndRemoveDevice(final String phoneNumber, final String crossRefId) {
        logger.info("Stop device monitoring of the phone: {}", phoneNumber);
        try {
            synchronized (SYNC) {
                provider.MonitorStop(crossRefId);
            }
        } catch (CstaException | RuntimeException e) {
            logger.error(format("An error occurred during stopping to monitor the phone: %s", phoneNumber), e.getMessage());
        }
        synchronized (SYNC) {
            provider.removeDevice(phoneNumber);
        }
    }

    public void restart() {
        logger.warn("Restart the CSTA server: {}:{}", address, port);
        disconnect();
        connect(listener);
        logger.warn("Finished restarting the CSTA server: {}:{}", address, port);
    }

    public void disconnect() {
        logger.info("Stop the heardbeat and disconnect");
        try {
            synchronized (SYNC) {
                if (!provider.removeEventListener(listener)) {
                    logger.warn("The CSTA listener was not removed successfully");
                }
                provider.endHeartbeat();
                provider.disconnectFromSystem();
            }
        } catch (CstaException e) {
            logger.error(format("An error occurred during disconnecting from the CSTA server: %s:%s", address, port), e);
        }
    }

    @Override
    public void close() throws IOException {
        disconnect();
    }

    public CstaDeviceData getData(String phoneNumber) {
        return monitoredPhones.get(phoneNumber);
    }

    public Set<String> getMonitoredPhones() {
        return new HashSet<>(monitoredPhones.keySet());
    }
}
