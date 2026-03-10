package com.catlava.plugin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and manages the plugin configuration from application.yml
 * Uses Spring's configuration properties binding for proper integration
 */
@Component
public class PluginConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(PluginConfigLoader.class);
    private static final String CONFIG_PATH = "classpath:application.yml";

    private final ResourceLoader resourceLoader;
    private final ObjectMapper yamlMapper;
    private PluginConfig config;

    public PluginConfigLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Create default plugin configuration
     */
    public static PluginConfig createDefaultConfig() {
        PluginConfig config = new PluginConfig();
        config.setEnabled(true);
        
        // Default auth config
        PluginConfig.AuthConfig authConfig = new PluginConfig.AuthConfig();
        authConfig.setEnabled(true);
        authConfig.setApiKeys(List.of("default-api-key"));
        config.setAuth(authConfig);
        
        // Default rate limit config
        PluginConfig.RateLimitConfig rateLimitConfig = new PluginConfig.RateLimitConfig();
        rateLimitConfig.setEnabled(true);
        rateLimitConfig.setRequestsPerMinute(60);
        rateLimitConfig.setBurst(10);
        config.setRateLimit(rateLimitConfig);
        
        // Default queue config
        PluginConfig.QueueConfig queueConfig = new PluginConfig.QueueConfig();
        queueConfig.setMaxSize(10000);
        queueConfig.setDefaultVolume(100);
        queueConfig.setPersistQueue(false);
        config.setQueue(queueConfig);
        
        // Default lyrics config
        PluginConfig.LyricsConfig lyricsConfig = new PluginConfig.LyricsConfig();
        lyricsConfig.setEnabled(true);
        lyricsConfig.setProviders(List.of("lyrics"));
        lyricsConfig.setCacheEnabled(true);
        lyricsConfig.setCacheDurationMinutes(60);
        config.setLyrics(lyricsConfig);
        
        // Default logging config
        PluginConfig.LoggingConfig loggingConfig = new PluginConfig.LoggingConfig();
        loggingConfig.setEnabled(true);
        loggingConfig.setLogRequests(true);
        loggingConfig.setLogResponses(false);
        loggingConfig.setLogLevel("INFO");
        loggingConfig.setLogFile("./logs/catlava.log");
        config.setLogging(loggingConfig);
        
        // Add default equalizer presets
        Map<String, List<PluginConfig.EqualizerBand>> presets = new HashMap<>();
        
        // Bass Boost preset
        List<PluginConfig.EqualizerBand> bassBoost = new ArrayList<>();
        bassBoost.add(new PluginConfig.EqualizerBand(0, 0.2f));
        bassBoost.add(new PluginConfig.EqualizerBand(1, 0.15f));
        bassBoost.add(new PluginConfig.EqualizerBand(2, 0.1f));
        presets.put("bass_boost", bassBoost);
        
        // Treble Boost preset
        List<PluginConfig.EqualizerBand> trebleBoost = new ArrayList<>();
        trebleBoost.add(new PluginConfig.EqualizerBand(12, 0.2f));
        trebleBoost.add(new PluginConfig.EqualizerBand(13, 0.15f));
        trebleBoost.add(new PluginConfig.EqualizerBand(14, 0.1f));
        presets.put("treble_boost", trebleBoost);
        
        // Flat preset
        List<PluginConfig.EqualizerBand> flat = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            flat.add(new PluginConfig.EqualizerBand(i, 0.0f));
        }
        presets.put("flat", flat);
        
        // Rock preset
        List<PluginConfig.EqualizerBand> rock = new ArrayList<>();
        rock.add(new PluginConfig.EqualizerBand(0, 0.3f));
        rock.add(new PluginConfig.EqualizerBand(1, 0.25f));
        rock.add(new PluginConfig.EqualizerBand(2, 0.2f));
        rock.add(new PluginConfig.EqualizerBand(3, 0.1f));
        rock.add(new PluginConfig.EqualizerBand(4, -0.1f));
        rock.add(new PluginConfig.EqualizerBand(5, -0.2f));
        rock.add(new PluginConfig.EqualizerBand(6, -0.2f));
        rock.add(new PluginConfig.EqualizerBand(7, -0.1f));
        rock.add(new PluginConfig.EqualizerBand(8, 0.1f));
        rock.add(new PluginConfig.EqualizerBand(9, 0.2f));
        rock.add(new PluginConfig.EqualizerBand(10, 0.25f));
        rock.add(new PluginConfig.EqualizerBand(11, 0.3f));
        rock.add(new PluginConfig.EqualizerBand(12, 0.3f));
        rock.add(new PluginConfig.EqualizerBand(13, 0.25f));
        rock.add(new PluginConfig.EqualizerBand(14, 0.2f));
        presets.put("rock", rock);
        
        // Pop preset
        List<PluginConfig.EqualizerBand> pop = new ArrayList<>();
        pop.add(new PluginConfig.EqualizerBand(0, -0.1f));
        pop.add(new PluginConfig.EqualizerBand(1, 0.1f));
        pop.add(new PluginConfig.EqualizerBand(2, 0.2f));
        pop.add(new PluginConfig.EqualizerBand(3, 0.3f));
        pop.add(new PluginConfig.EqualizerBand(4, 0.3f));
        pop.add(new PluginConfig.EqualizerBand(5, 0.2f));
        pop.add(new PluginConfig.EqualizerBand(6, 0.1f));
        pop.add(new PluginConfig.EqualizerBand(7, 0.0f));
        pop.add(new PluginConfig.EqualizerBand(8, -0.1f));
        pop.add(new PluginConfig.EqualizerBand(9, -0.1f));
        pop.add(new PluginConfig.EqualizerBand(10, 0.0f));
        pop.add(new PluginConfig.EqualizerBand(11, 0.1f));
        pop.add(new PluginConfig.EqualizerBand(12, 0.1f));
        pop.add(new PluginConfig.EqualizerBand(13, 0.0f));
        pop.add(new PluginConfig.EqualizerBand(14, -0.1f));
        presets.put("pop", pop);
        
        config.setFilterPresets(presets);
        
        return config;
    }

    @PostConstruct
    public void loadConfig() {
        try {
            Resource resource = resourceLoader.getResource(CONFIG_PATH);
            if (!resource.exists()) {
                log.warn("application.yml not found, using default configuration");
                this.config = createDefaultConfig();
                return;
            }

            Map<String, Object> yamlMap = yamlMapper.readValue(resource.getInputStream(), Map.class);
            
            // Navigate to plugins.catlava
            Map<String, Object> plugins = (Map<String, Object>) yamlMap.get("plugins");
            if (plugins == null) {
                log.warn("No plugins section found in application.yml, using default configuration");
                this.config = createDefaultConfig();
                return;
            }

            Map<String, Object> catlava = (Map<String, Object>) plugins.get("catlava");
            if (catlava == null) {
                log.warn("No catlava plugin configuration found, using default configuration");
                this.config = createDefaultConfig();
                return;
            }

            this.config = parseConfig(catlava);
            log.info("CatLava plugin configuration loaded successfully");

        } catch (IOException e) {
            log.error("Failed to load plugin configuration, using defaults", e);
            this.config = createDefaultConfig();
        }
    }

    @NotNull
    private PluginConfig parseConfig(Map<String, Object> catlava) {
        PluginConfig config = new PluginConfig();

        // Parse enabled
        if (catlava.containsKey("enabled")) {
            config.setEnabled((Boolean) catlava.get("enabled"));
        }

        // Parse auth config
        if (catlava.containsKey("auth")) {
            Map<String, Object> authMap = (Map<String, Object>) catlava.get("auth");
            PluginConfig.AuthConfig authConfig = new PluginConfig.AuthConfig();
            if (authMap.containsKey("enabled")) {
                authConfig.setEnabled((Boolean) authMap.get("enabled"));
            }
            if (authMap.containsKey("apiKeys")) {
                authConfig.setApiKeys(new ArrayList<>((List<String>) authMap.get("apiKeys")));
            }
            if (authMap.containsKey("ipWhitelist")) {
                authConfig.setIpWhitelist(new ArrayList<>((List<String>) authMap.get("ipWhitelist")));
            }
            if (authMap.containsKey("ipBlacklist")) {
                authConfig.setIpBlacklist(new ArrayList<>((List<String>) authMap.get("ipBlacklist")));
            }
            config.setAuth(authConfig);
        }

        // Parse rate limit config
        if (catlava.containsKey("rateLimit")) {
            Map<String, Object> rateLimitMap = (Map<String, Object>) catlava.get("rateLimit");
            PluginConfig.RateLimitConfig rateLimitConfig = new PluginConfig.RateLimitConfig();
            if (rateLimitMap.containsKey("enabled")) {
                rateLimitConfig.setEnabled((Boolean) rateLimitMap.get("enabled"));
            }
            if (rateLimitMap.containsKey("requestsPerMinute")) {
                rateLimitConfig.setRequestsPerMinute((Integer) rateLimitMap.get("requestsPerMinute"));
            }
            if (rateLimitMap.containsKey("burst")) {
                rateLimitConfig.setBurst((Integer) rateLimitMap.get("burst"));
            }
            config.setRateLimit(rateLimitConfig);
        }

        // Parse queue config
        if (catlava.containsKey("queue")) {
            Map<String, Object> queueMap = (Map<String, Object>) catlava.get("queue");
            PluginConfig.QueueConfig queueConfig = new PluginConfig.QueueConfig();
            if (queueMap.containsKey("maxSize")) {
                queueConfig.setMaxSize((Integer) queueMap.get("maxSize"));
            }
            if (queueMap.containsKey("defaultVolume")) {
                queueConfig.setDefaultVolume((Integer) queueMap.get("defaultVolume"));
            }
            if (queueMap.containsKey("persistQueue")) {
                queueConfig.setPersistQueue((Boolean) queueMap.get("persistQueue"));
            }
            config.setQueue(queueConfig);
        }

        // Parse lyrics config
        if (catlava.containsKey("lyrics")) {
            Map<String, Object> lyricsMap = (Map<String, Object>) catlava.get("lyrics");
            PluginConfig.LyricsConfig lyricsConfig = new PluginConfig.LyricsConfig();
            if (lyricsMap.containsKey("enabled")) {
                lyricsConfig.setEnabled((Boolean) lyricsMap.get("enabled"));
            }
            if (lyricsMap.containsKey("providers")) {
                lyricsConfig.setProviders(new ArrayList<>((List<String>) lyricsMap.get("providers")));
            }
            if (lyricsMap.containsKey("cacheEnabled")) {
                lyricsConfig.setCacheEnabled((Boolean) lyricsMap.get("cacheEnabled"));
            }
            if (lyricsMap.containsKey("cacheDurationMinutes")) {
                lyricsConfig.setCacheDurationMinutes((Integer) lyricsMap.get("cacheDurationMinutes"));
            }
            config.setLyrics(lyricsConfig);
        }

        // Parse logging config
        if (catlava.containsKey("logging")) {
            Map<String, Object> loggingMap = (Map<String, Object>) catlava.get("logging");
            PluginConfig.LoggingConfig loggingConfig = new PluginConfig.LoggingConfig();
            if (loggingMap.containsKey("enabled")) {
                loggingConfig.setEnabled((Boolean) loggingMap.get("enabled"));
            }
            if (loggingMap.containsKey("logRequests")) {
                loggingConfig.setLogRequests((Boolean) loggingMap.get("logRequests"));
            }
            if (loggingMap.containsKey("logResponses")) {
                loggingConfig.setLogResponses((Boolean) loggingMap.get("logResponses"));
            }
            if (loggingMap.containsKey("logLevel")) {
                loggingConfig.setLogLevel((String) loggingMap.get("logLevel"));
            }
            if (loggingMap.containsKey("logFile")) {
                loggingConfig.setLogFile((String) loggingMap.get("logFile"));
            }
            config.setLogging(loggingConfig);
        }

        // Parse filter presets
        if (catlava.containsKey("filters")) {
            Map<String, Object> filtersMap = (Map<String, Object>) catlava.get("filters");
            if (filtersMap.containsKey("equalizerPresets")) {
                Map<String, Object> presetsMap = (Map<String, Object>) filtersMap.get("equalizerPresets");
                Map<String, List<PluginConfig.EqualizerBand>> presets = new HashMap<>();
                
                for (Map.Entry<String, Object> entry : presetsMap.entrySet()) {
                    List<Map<String, Object>> bandsList = (List<Map<String, Object>>) entry.getValue();
                    List<PluginConfig.EqualizerBand> bands = new ArrayList<>();
                    
                    for (Map<String, Object> bandMap : bandsList) {
                        int band = (Integer) bandMap.get("band");
                        float gain = ((Number) bandMap.get("gain")).floatValue();
                        bands.add(new PluginConfig.EqualizerBand(band, gain));
                    }
                    presets.put(entry.getKey(), bands);
                }
                config.setFilterPresets(presets);
            }
        }

        return config;
    }

    public PluginConfig getConfig() {
        return config;
    }

    public void reloadConfig() {
        loadConfig();
    }
}

