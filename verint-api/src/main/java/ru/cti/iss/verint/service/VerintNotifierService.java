package ru.cti.iss.verint.service;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerintNotifierService {
    private static final Logger logger = LoggerFactory.getLogger(VerintNotifierService.class);

    // "ContactEvent: Succeeded";
    private static final Pattern PATTERN = Pattern.compile("ContactEvent: (\\w+)", Pattern.MULTILINE | Pattern.DOTALL);

    private final VerintMonitoringService monitoringService;
    private final String serverURL;
    private final OkHttpClient client;

    public VerintNotifierService(VerintMonitoringService monitoringService, String serverURL, OkHttpClient client) {
        this.monitoringService = monitoringService;
        this.serverURL = serverURL;
        this.client = client;
    }

    public void requestPersisting(final String extension) {
        try {
            final Request request = new Request.Builder()
                    .url(serverURL + extension)
                    .get()
                    .build();
            logger.info("Send request for verint persist to {}", extension);

            final Response response = client.newCall(request).execute();
            logger.info("Send request for verint persist to {} finish", extension);
            if (response.isSuccessful()) {
                final String status = getResponseStatus(response);

                if ("Succeeded".equals(status)) {
                    logger.info("Conversation with device {} will be saved.", extension);
                } else {
                    logger.error("Verint request to persist extension {} return with ContactEvent: {}", extension, status);
                    monitoringService.increaseVerintWsError();
                }

            } else {
                logger.error("Verint request to persist extension: {} return with HTTP status {}", extension, response.message());
                monitoringService.increaseVerintWsError();
            }


        } catch (final Exception e) {
            logger.error("Verint request for device {} failed", extension, e);
            monitoringService.increaseVerintWsError();
        }
    }

    private String getResponseStatus(final Response response) throws IOException {
        final String answer = response.body().string();
        final Matcher matcher = PATTERN.matcher(answer);
        return matcher.find() ? matcher.group(1) : "<status not found>";
    }
}
