package com.catlava.plugin.rest;

import com.catlava.plugin.CatLavaPlugin;
import com.catlava.plugin.audio.CustomFilters;
import com.catlava.plugin.audio.LyricsService;
import com.catlava.plugin.config.PluginConfig;
import com.catlava.plugin.queue.QueueManager;
import dev.arbjerg.lavalink.api.RestController;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for plugin statistics and management.
 * Provides API endpoints for monitoring and managing the plugin.
 */
@RestController
public class PluginStatsController implements RestController {

    private static final Logger log = LoggerFactory.getLogger(PluginStatsController.class);
    
    private final CatLavaPlugin plugin;
    private final PluginConfig config;
    private final QueueManager queueManager;
    private final LyricsService lyricsService;
    private final CustomFilters customFilters;

    public PluginStatsController(
            CatLavaPlugin plugin,
            PluginConfig config,
            QueueManager queueManager,
            LyricsService lyricsService,
            CustomFilters customFilters) {
        this.plugin = plugin;
        this.config = config;
        this.queueManager = queueManager;
        this.lyricsService = lyricsService;
        this.customFilters = customFilters;
    }

    /**
     * Get plugin statistics
     * GET /v4/catlava/stats
     */
    @GetMapping("/v4/catlava/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        CatLavaPlugin.PluginStats stats = plugin.getStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("plugin", "CatLava");
        response.put("version", "1.0.0");
        response.put("enabled", plugin.isEnabled());
        
        // Queue stats
        Map<String, Object> queueStats = new HashMap<>();
        queueStats.put("activeQueues", stats.activeQueues());
        queueStats.put("totalTracks", stats.totalTracksInQueue());
        queueStats.put("maxSize", config.getQueue().getMaxSize());
        queueStats.put("defaultVolume", config.getQueue().getDefaultVolume());
        response.put("queue", queueStats);
        
        // Lyrics stats
        Map<String, Object> lyricsStats = new HashMap<>();
        lyricsStats.put("enabled", config.getLyrics().isEnabled());
        lyricsStats.put("cachedLyrics", stats.cachedLyrics());
        lyricsStats.put("providers", config.getLyrics().getProviders());
        response.put("lyrics", lyricsStats);
        
        // Filter stats
        Map<String, Object> filterStats = new HashMap<>();
        filterStats.put("availablePresets", customFilters.getAvailablePresets());
        response.put("filters", filterStats);
        
        // Auth stats
        Map<String, Object> authStats = new HashMap<>();
        authStats.put("enabled", config.getAuth().isEnabled());
        authStats.put("configuredApiKeys", stats.configuredApiKeys());
        authStats.put("ipWhitelistSize", config.getAuth().getIpWhitelist().size());
        authStats.put("ipBlacklistSize", config.getAuth().getIpBlacklist().size());
        response.put("auth", authStats);
        
        // Rate limit stats
        Map<String, Object> rateLimitStats = new HashMap<>();
        rateLimitStats.put("enabled", config.getRateLimit().isEnabled());
        rateLimitStats.put("requestsPerMinute", config.getRateLimit().getRequestsPerMinute());
        rateLimitStats.put("burst", config.getRateLimit().getBurst());
        response.put("rateLimit", rateLimitStats);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get system information
     * GET /v4/catlava/system
     */
    @GetMapping("/v4/catlava/system")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        
        Map<String, Object> response = new HashMap<>();
        
        // JVM info
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("vmName", runtimeBean.getVmName());
        jvm.put("vmVendor", runtimeBean.getVmVendor());
        jvm.put("vmVersion", runtimeBean.getVmVersion());
        jvm.put("uptimeMs", runtimeBean.getUptime());
        response.put("jvm", jvm);
        
        // Memory info
        Map<String, Object> memory = new HashMap<>();
        memory.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
        memory.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
        memory.put("heapCommitted", memoryBean.getHeapMemoryUsage().getCommitted());
        memory.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
        response.put("memory", memory);
        
        // Thread info
        Map<String, Object> threads = new HashMap<>();
        threads.put("count", ManagementFactory.getThreadMXBean().getThreadCount());
        threads.put("peak", ManagementFactory.getThreadMXBean().getPeakThreadCount());
        response.put("threads", threads);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get plugin configuration
     * GET /v4/catlava/config
     */
    @GetMapping("/v4/catlava/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", config.isEnabled());
        response.put("auth", Map.of(
            "enabled", config.getAuth().isEnabled(),
            "hasApiKeys", !config.getAuth().getApiKeys().isEmpty(),
            "ipWhitelistCount", config.getAuth().getIpWhitelist().size(),
            "ipBlacklistCount", config.getAuth().getIpBlacklist().size()
        ));
        response.put("rateLimit", Map.of(
            "enabled", config.getRateLimit().isEnabled(),
            "requestsPerMinute", config.getRateLimit().getRequestsPerMinute(),
            "burst", config.getRateLimit().getBurst()
        ));
        response.put("queue", Map.of(
            "maxSize", config.getQueue().getMaxSize(),
            "defaultVolume", config.getQueue().getDefaultVolume(),
            "persistQueue", config.getQueue().isPersistQueue()
        ));
        response.put("lyrics", Map.of(
            "enabled", config.getLyrics().isEnabled(),
            "providers", config.getLyrics().getProviders(),
            "cacheEnabled", config.getLyrics().isCacheEnabled()
        ));
        response.put("logging", Map.of(
            "enabled", config.getLogging().isEnabled(),
            "logLevel", config.getLogging().getLogLevel(),
            "logRequests", config.getLogging().isLogRequests(),
            "logResponses", config.getLogging().isLogResponses()
        ));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Reload plugin configuration
     * POST /v4/catlava/config/reload
     */
    @PostMapping("/v4/catlava/config/reload")
    public ResponseEntity<Map<String, Object>> reloadConfig() {
        try {
            plugin.reloadConfig();
            log.info("Plugin configuration reloaded");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Configuration reloaded successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to reload configuration", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to reload configuration: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /v4/catlava/health
     */
    @GetMapping("/v4/catlava/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("plugin", "CatLava");
        response.put("version", "1.0.0");
        response.put("enabled", plugin.isEnabled());
        
        // Check components
        Map<String, Boolean> components = new HashMap<>();
        components.put("queue", queueManager != null);
        components.put("lyrics", lyricsService != null);
        components.put("filters", customFilters != null);
        components.put("config", config != null);
        response.put("components", components);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get available filter presets
     * GET /v4/catlava/filters/presets
     */
    @GetMapping("/v4/catlava/filters/presets")
    public ResponseEntity<Map<String, Object>> getFilterPresets() {
        return ResponseEntity.ok(Map.of(
            "presets", customFilters.getAvailablePresets()
        ));
    }

    @Override
    public @NotNull String getRoute() {
        return "/v4/catlava";
    }
}

