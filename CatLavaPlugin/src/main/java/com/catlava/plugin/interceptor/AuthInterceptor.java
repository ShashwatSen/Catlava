package com.catlava.plugin.interceptor;

import com.catlava.plugin.config.PluginConfig;
import dev.arbjerg.lavalink.api.RestInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interceptor for authentication and authorization.
 * Validates API keys and IP addresses.
 */
@Component
public class AuthInterceptor implements RestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final PluginConfig config;

    public AuthInterceptor(PluginConfig config) {
        this.config = config;
    }

    @NotNull
    @Override
    public Map<String, String> onRequest(@NotNull Map<String, String> headers, @NotNull String path, @NotNull String method) {
        // Skip auth if disabled
        if (!config.getAuth().isEnabled()) {
            return headers;
        }

        // Skip auth for certain paths (health checks, etc.)
        if (isPublicPath(path)) {
            return headers;
        }

        // Get client IP from headers (might be set by proxy)
        String clientIp = getClientIp(headers);

        // Check IP blacklist
        if (isIpBlacklisted(clientIp)) {
            log.warn("Blocked request from blacklisted IP: {}", clientIp);
            throw new SecurityException("Access denied: Your IP is blacklisted");
        }

        // Check IP whitelist (if configured)
        if (!config.getAuth().getIpWhitelist().isEmpty() && !isIpWhitelisted(clientIp)) {
            log.warn("Blocked request from non-whitelisted IP: {}", clientIp);
            throw new SecurityException("Access denied: Your IP is not whitelisted");
        }

        // Check API key
        String authHeader = headers.get(AUTH_HEADER);
        if (authHeader == null) {
            log.warn("Missing authorization header from IP: {}", clientIp);
            throw new SecurityException("Access denied: Missing authorization header");
        }

        if (!validateApiKey(authHeader)) {
            log.warn("Invalid API key from IP: {}", clientIp);
            throw new SecurityException("Access denied: Invalid API key");
        }

        // Add authenticated user info to headers
        Map<String, String> modifiedHeaders = new HashMap<>(headers);
        modifiedHeaders.put("X-Authenticated", "true");
        modifiedHeaders.put("X-Client-Ip", clientIp);

        log.debug("Authenticated request from IP: {}", clientIp);
        return modifiedHeaders;
    }

    private boolean isPublicPath(String path) {
        // Define paths that don't require authentication
        return path.startsWith("/v4/health") || 
               path.startsWith("/metrics") ||
               path.equals("/");
    }

    private String getClientIp(Map<String, String> headers) {
        // Check for forwarded headers (in case behind a proxy)
        String forwarded = headers.get("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            // Take the first IP in the chain
            return forwarded.split(",")[0].trim();
        }

        String realIp = headers.get("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        return "unknown";
    }

    private boolean isIpBlacklisted(String clientIp) {
        List<String> blacklist = config.getAuth().getIpBlacklist();
        return blacklist.contains(clientIp);
    }

    private boolean isIpWhitelisted(String clientIp) {
        List<String> whitelist = config.getAuth().getIpWhitelist();
        return whitelist.contains(clientIp);
    }

    private boolean validateApiKey(String authHeader) {
        List<String> validKeys = config.getAuth().getApiKeys();
        
        if (validKeys.isEmpty()) {
            // If no keys configured, allow all (for development)
            return true;
        }

        // Handle Bearer token format
        String token = authHeader;
        if (authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        }

        return validKeys.contains(token);
    }

    @Override
    public void onResponse(@NotNull String path, @NotNull String method, int statusCode, @NotNull String body, long timeTakenMs) {
        // No action needed on response
    }

    @Override
    public void onError(@NotNull String path, @NotNull String method, @NotNull Throwable error, long timeTakenMs) {
        log.error("Auth error on {} {}: {}", method, path, error.getMessage());
    }
}

