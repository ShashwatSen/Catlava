package com.catlava.plugin.interceptor;

import com.catlava.plugin.config.PluginConfig;
import dev.arbjerg.lavalink.api.Interaction;
import dev.arbjerg.lavalink.api.RestInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for logging HTTP requests and responses.
 */
@Component
public class RequestLoggingInterceptor implements RestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private final PluginConfig config;

    public RequestLoggingInterceptor(PluginConfig config) {
        this.config = config;
    }

    @NotNull
    @Override
    public Map<String, String> onRequest(@NotNull Map<String, String> headers, @NotNull String path, @NotNull String method) {
        if (!config.getLogging().isEnabled() || !config.getLogging().isLogRequests()) {
            return headers;
        }

        String requestId = generateRequestId();
        long timestamp = Instant.now().toEpochMilli();

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("timestamp", timestamp);
        logData.put("method", method);
        logData.put("path", path);
        logData.put("type", "REQUEST");

        // Log headers if enabled
        if (config.getLogging().isLogRequests()) {
            logData.put("headers", headers);
        }

        log.info("Incoming Request: {}", formatLog(logData));

        // Add request ID to headers for tracking
        Map<String, String> modifiedHeaders = new HashMap<>(headers);
        modifiedHeaders.put("X-Request-Id", requestId);

        return modifiedHeaders;
    }

    @Override
    public void onResponse(@NotNull String path, @NotNull String method, int statusCode, @NotNull String body, long timeTakenMs) {
        if (!config.getLogging().isEnabled() || !config.getLogging().isLogResponses()) {
            return;
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("type", "RESPONSE");
        logData.put("method", method);
        logData.put("path", path);
        logData.put("statusCode", statusCode);
        logData.put("timeTakenMs", timeTakenMs);

        // Truncate body if too long
        String logBody = body;
        if (body.length() > config.getLogging().getLogFile().length()) {
            logBody = body.substring(0, 100) + "... [truncated]";
        }
        logData.put("body", logBody);

        log.info("Response: {}", formatLog(logData));
    }

    @Override
    public void onError(@NotNull String path, @NotNull String method, @NotNull Throwable error, long timeTakenMs) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("type", "ERROR");
        logData.put("method", method);
        logData.put("path", path);
        logData.put("timeTakenMs", timeTakenMs);
        logData.put("error", error.getMessage());
        logData.put("errorClass", error.getClass().getSimpleName());

        log.error("Request Error: {}", formatLog(logData), error);
    }

    private String generateRequestId() {
        return "req-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    private String formatLog(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        data.forEach((key, value) -> {
            sb.append(key).append("=").append(value).append(", ");
        });
        if (sb.length() > 2) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("]");
        return sb.toString();
    }
}

