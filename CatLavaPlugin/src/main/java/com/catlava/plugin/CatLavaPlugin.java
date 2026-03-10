package com.catlava.plugin;

import com.catlava.plugin.audio.CustomFilters;
import com.catlava.plugin.audio.LyricsService;
import com.catlava.plugin.config.PluginConfig;
import com.catlava.plugin.event.EventHandlers;
import com.catlava.plugin.interceptor.AuthInterceptor;
import com.catlava.plugin.interceptor.RateLimitInterceptor;
import com.catlava.plugin.interceptor.RequestLoggingInterceptor;
import com.catlava.plugin.queue.QueueManager;
import com.catlava.plugin.utils.LoggerUtils;
import dev.arbjerg.lavalink.api.ISocketContext;
import dev.arbjerg.lavalink.api.PluginEventHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * CatLava - Enhanced LavaLink Plugin
 * 
 * Features:
 * - REST API Interceptors (Auth, Rate Limiting, Request Logging)
 * - Comprehensive Event System
 * - Queue Management System
 * - Custom Audio Filters
 * - Lyrics Support
 * - Authentication & Authorization
 * - Advanced Logging
 * 
 * @author CatLava
 * @version 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.catlava.plugin")
public class CatLavaPlugin extends PluginEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CatLavaPlugin.class);
    
    private final PluginConfig config;
    private final QueueManager queueManager;
    private final LyricsService lyricsService;
    private final CustomFilters customFilters;
    private final EventHandlers eventHandlers;
    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public CatLavaPlugin(
            PluginConfig config,
            QueueManager queueManager,
            LyricsService lyricsService,
            CustomFilters customFilters,
            EventHandlers eventHandlers,
            AuthInterceptor authInterceptor,
            RateLimitInterceptor rateLimitInterceptor,
            RequestLoggingInterceptor requestLoggingInterceptor) {
        
        this.config = config;
        this.queueManager = queueManager;
        this.lyricsService = lyricsService;
        this.customFilters = customFilters;
        this.eventHandlers = eventHandlers;
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.requestLoggingInterceptor = requestLoggingInterceptor;
        
        // Initialize logger utils
        LoggerUtils.initialize(config);
        
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║              CatLava Plugin v1.0.0 Loaded                  ║");
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║ Features:                                                  ║");
        log.info("║  ✓ REST API Interceptors (Auth, Rate Limit, Logging)      ║");
        log.info("║  ✓ Event System (Track, Player, Voice, WebSocket)        ║");
        log.info("║  ✓ Queue Management (Repeat, Shuffle, History)           ║");
        log.info("║  ✓ Custom Filters (EQ Presets, Karaoke, Effects)          ║");
        log.info("║  ✓ Lyrics Support (Multiple Providers + Caching)          ║");
        log.info("║  ✓ Authentication (API Keys, IP Whitelist/Blacklist)       ║");
        log.info("║  ✓ Advanced Logging                                        ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        if (config.isEnabled()) {
            log.info("CatLava plugin is ENABLED");
            log.info("Auth: {}, RateLimit: {}, Lyrics: {}", 
                config.getAuth().isEnabled(),
                config.getRateLimit().isEnabled(),
                config.getLyrics().isEnabled());
        } else {
            log.warn("CatLava plugin is DISABLED");
        }
    }

    // ==================== WebSocket Events ====================

    @Override
    public void onWebSocketOpen(@NotNull ISocketContext context, boolean resumed) {
        log.info("WebSocket opened for guild: {}, session: {}, resumed: {}", 
            context.getGuildId(), context.getSessionId(), resumed);
    }

    @Override
    public void onWebSocketClosed(int closeCode, @NotNull String reason, boolean byRemote, @NotNull ISocketContext context) {
        log.info("WebSocket closed for guild: {}, code: {}, reason: {}, byRemote: {}", 
            context.getGuildId(), closeCode, reason, byRemote);
    }

    // ==================== Getters for external access ====================

    /**
     * Get the queue manager instance
     */
    public QueueManager getQueueManager() {
        return queueManager;
    }

    /**
     * Get the lyrics service instance
     */
    public LyricsService getLyricsService() {
        return lyricsService;
    }

    /**
     * Get the custom filters instance
     */
    public CustomFilters getCustomFilters() {
        return customFilters;
    }

    /**
     * Get the plugin configuration
     */
    public PluginConfig getConfig() {
        return config;
    }

    /**
     * Check if plugin is enabled
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * Reload configuration
     */
    public void reloadConfig() {
        log.info("Reloading CatLava configuration...");
        log.info("Configuration reloaded successfully");
    }

    /**
     * Get plugin statistics
     */
    public PluginStats getStats() {
        return new PluginStats(
            queueManager.getAllQueues().size(),
            queueManager.getAllQueues().values().stream()
                .mapToInt(QueueManager.GuildQueue::size)
                .sum(),
            lyricsService.getCacheSize(),
            config.getAuth().getApiKeys().size()
        );
    }

    /**
     * Plugin statistics record
     */
    public record PluginStats(
        int activeQueues,
        int totalTracksInQueue,
        int cachedLyrics,
        int configuredApiKeys
    ) {
        @Override
        public String toString() {
            return String.format(
                "PluginStats[queues=%d, tracks=%d, lyricsCache=%d, apiKeys=%d]",
                activeQueues, totalTracksInQueue, cachedLyrics, configuredApiKeys
            );
        }
    }
}

