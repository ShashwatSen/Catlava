package com.catlava.plugin.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main configuration class for the CatLava plugin.
 * Loaded from application.yml under plugins.catlava
 */
@Component
public class PluginConfig {

    private boolean enabled = true;
    private AuthConfig auth = new AuthConfig();
    private RateLimitConfig rateLimit = new RateLimitConfig();
    private QueueConfig queue = new QueueConfig();
    private LyricsConfig lyrics = new LyricsConfig();
    private LoggingConfig logging = new LoggingConfig();
    private Map<String, List<EqualizerBand>> filterPresets = new HashMap<>();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    public QueueConfig getQueue() {
        return queue;
    }

    public void setQueue(QueueConfig queue) {
        this.queue = queue;
    }

    public LyricsConfig getLyrics() {
        return lyrics;
    }

    public void setLyrics(LyricsConfig lyrics) {
        this.lyrics = lyrics;
    }

    public LoggingConfig getLogging() {
        return logging;
    }

    public void setLogging(LoggingConfig logging) {
        this.logging = logging;
    }

    public Map<String, List<EqualizerBand>> getFilterPresets() {
        return filterPresets;
    }

    public void setFilterPresets(Map<String, List<EqualizerBand>> filterPresets) {
        this.filterPresets = filterPresets;
    }

    /**
     * Authentication configuration
     */
    public static class AuthConfig {
        private boolean enabled = true;
        private List<String> apiKeys = new ArrayList<>();
        private List<String> ipWhitelist = new ArrayList<>();
        private List<String> ipBlacklist = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getApiKeys() {
            return apiKeys;
        }

        public void setApiKeys(List<String> apiKeys) {
            this.apiKeys = apiKeys;
        }

        public List<String> getIpWhitelist() {
            return ipWhitelist;
        }

        public void setIpWhitelist(List<String> ipWhitelist) {
            this.ipWhitelist = ipWhitelist;
        }

        public List<String> getIpBlacklist() {
            return ipBlacklist;
        }

        public void setIpBlacklist(List<String> ipBlacklist) {
            this.ipBlacklist = ipBlacklist;
        }
    }

    /**
     * Rate limiting configuration
     */
    public static class RateLimitConfig {
        private boolean enabled = true;
        private int requestsPerMinute = 60;
        private int burst = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public int getBurst() {
            return burst;
        }

        public void setBurst(int burst) {
            this.burst = burst;
        }
    }

    /**
     * Queue configuration
     */
    public static class QueueConfig {
        private int maxSize = 10000;
        private int defaultVolume = 100;
        private boolean persistQueue = false;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getDefaultVolume() {
            return defaultVolume;
        }

        public void setDefaultVolume(int defaultVolume) {
            this.defaultVolume = defaultVolume;
        }

        public boolean isPersistQueue() {
            return persistQueue;
        }

        public void setPersistQueue(boolean persistQueue) {
            this.persistQueue = persistQueue;
        }
    }

    /**
     * Lyrics configuration
     */
    public static class LyricsConfig {
        private boolean enabled = true;
        private List<String> providers = List.of("lyrics");
        private boolean cacheEnabled = true;
        private int cacheDurationMinutes = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getProviders() {
            return providers;
        }

        public void setProviders(List<String> providers) {
            this.providers = providers;
        }

        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public int getCacheDurationMinutes() {
            return cacheDurationMinutes;
        }

        public void setCacheDurationMinutes(int cacheDurationMinutes) {
            this.cacheDurationMinutes = cacheDurationMinutes;
        }
    }

    /**
     * Logging configuration
     */
    public static class LoggingConfig {
        private boolean enabled = true;
        private boolean logRequests = true;
        private boolean logResponses = false;
        private String logLevel = "INFO";
        private String logFile = "./logs/catlava.log";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isLogRequests() {
            return logRequests;
        }

        public void setLogRequests(boolean logRequests) {
            this.logRequests = logRequests;
        }

        public boolean isLogResponses() {
            return logResponses;
        }

        public void setLogResponses(boolean logResponses) {
            this.logResponses = logResponses;
        }

        public String getLogLevel() {
            return logLevel;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }

        public String getLogFile() {
            return logFile;
        }

        public void setLogFile(String logFile) {
            this.logFile = logFile;
        }
    }

    /**
     * Equalizer band configuration
     */
    public static class EqualizerBand {
        private int band;
        private float gain;

        public EqualizerBand() {}

        public EqualizerBand(int band, float gain) {
            this.band = band;
            this.gain = gain;
        }

        public int getBand() {
            return band;
        }

        public void setBand(int band) {
            this.band = band;
        }

        public float getGain() {
            return gain;
        }

        public void setGain(float gain) {
            this.gain = gain;
        }
    }
}

