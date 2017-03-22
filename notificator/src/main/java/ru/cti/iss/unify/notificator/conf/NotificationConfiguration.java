package ru.cti.iss.unify.notificator.conf;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import ru.cti.iss.unify.notificator.service.MonitoringService;
import ru.cti.iss.unify.notificator.service.NotificatorInitService;
import ru.cti.iss.unify.notificator.service.PhoneService;
import ru.cti.iss.unify.notificator.service.WorkerService;
import ru.cti.iss.unify.notificator.service.csta.CstaListener;
import ru.cti.iss.unify.notificator.service.csta.CstaService;
import ru.cti.iss.verint.VerintConfiguration;
import ru.cti.iss.verint.service.VerintMonitoringService;
import ru.cti.iss.verint.service.VerintService;
import ru.cti.sipphone.pool.factory.PhonePooledObjectFactory;
import ru.cti.sipphone.pool.phone.GeneralSettings;
import ru.cti.sipphone.pool.phone.Phone;
import ru.cti.sipphone.pool.phone.PhoneSettings;

import javax.inject.Inject;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Configuration
@ComponentScan(basePackages = {
          "ru.cti.iss.unify.notificator.service.init",
          "ru.cti.iss.unify.notificator.service.monitoring"
})
@PropertySource ("classpath:sipsoftphone.properties")
@PropertySource ("classpath:unify.properties")
@Import(VerintConfiguration.class)
public class NotificationConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private Environment environment;

    @Inject
    private VerintConfiguration verintConfiguration;

    @Bean(destroyMethod = "close")
    public GenericObjectPool<Phone> phonePool() throws Exception {
        final Set<PhoneSettings> phoneSettings = phoneSettings();
        final GeneralSettings settings = generalSettings();
        final PooledObjectFactory<Phone> factory = new PhonePooledObjectFactory(phoneSettings, settings);

        final GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setJmxEnabled(false);
        config.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(10));
        config.setBlockWhenExhausted(false);
        config.setMinIdle(phoneSettings.size() / 2 + 1);
        config.setMaxTotal(phoneSettings.size());

        GenericObjectPool<Phone> pool = new GenericObjectPool<>(factory, config);
        pool.preparePool();
        return pool;
    }

    private Set<PhoneSettings> phoneSettings() {
        final Pattern numberPattern = Pattern.compile("^\\d{11}$");
        final Set<PhoneSettings> phoneSettingses = new HashSet<>();
        for (String number : environment.getProperty("teminal.phonenumbers").split(",")) {
            if (numberPattern.matcher(number).matches()) {
                phoneSettingses.add(new PhoneSettings(number, 0));
            } else {
                logger.warn("The phone: {} was skipped due to mismatching format: {}", number, numberPattern.pattern());
            }
        }
        return phoneSettingses;
    }

    public GeneralSettings generalSettings() {
        final String localAddress = environment.getProperty("teminal.localInterface.ipAddress");
        final String serverAddress = environment.getProperty("teminal.sipServer.ipAddress");
        final String mediaFile = environment.getProperty("audioFile");
        return new GeneralSettings(getMediaFileURL(mediaFile), localAddress, serverAddress);
    }

    @Bean
    public VerintMonitoringService monitoringService() {
        return new MonitoringService();
    }

    @Bean
    public VerintService verintService() {
        return new VerintService(verintConfiguration.verintDao(), verintConfiguration.verintNotifierService(), monitoringService());
    }

    @Bean(destroyMethod = "close")
    public WorkerService workerService() {
        int workerSize = environment.getProperty("teminal.phonenumbers").split(",").length;
        return new WorkerService(workerSize);
    }

    @Bean(destroyMethod = "close")
    public PhoneService phoneService() throws Exception {
        final String mediaFile = environment.getProperty("audioFile");
        int duration = estimateMediaFileDuration(mediaFile);
        return new PhoneService(duration, phonePool(), verintService(), workerService());
    }

    @Bean(destroyMethod = "disconnect")
    public CstaService cstaService() {
        final String address = environment.getProperty("unify.csta.serverIpAddress");
        final int port = environment.getProperty("unify.csta.serverPort", Integer.class);
        return new CstaService(address, port);
    }

    @Bean
    public NotificatorInitService notificatorInitService() throws Exception {
        CstaService cstaService = cstaService();
        cstaService.connect(new CstaListener(cstaService, phoneService()));
        return new NotificatorInitService(cstaService, verintService());
    }

    public URL getMediaFileURL(String mediaFile) {
        //fixme звук должен быть обязтельно закодирован в определенном формате!
        URL url = getClass().getClassLoader().getResource(Paths.get("media", mediaFile).toString());
        Objects.requireNonNull(url, "Cannot find the path to the media file.");
        return url;
    }

    public int estimateMediaFileDuration(String mediaFile) {
        URL url = getMediaFileURL(mediaFile);
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(url)) {
            AudioFormat format = stream.getFormat();
            long audioFileSize = Files.size(Paths.get(url.toURI()));
            return Math.round(audioFileSize / (format.getFrameSize() * format.getFrameRate())) + 1;
        } catch (Exception e) {
            throw new RuntimeException("Failed to estimate the media file duration", e);
        }
    }
}
