package com.catlava.plugin.rest;

import com.catlava.plugin.audio.LyricsService;
import com.catlava.plugin.config.PluginConfig;
import dev.arbjerg.lavalink.api.RestController;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for lyrics-related endpoints.
 * Provides API endpoints for fetching and managing lyrics.
 */
@RestController
public class LyricsController implements RestController {

    private static final Logger log = LoggerFactory.getLogger(LyricsController.class);
    
    private final LyricsService lyricsService;
    private final PluginConfig config;

    public LyricsController(LyricsService lyricsService, PluginConfig config) {
        this.lyricsService = lyricsService;
        this.config = config;
    }

    /**
     * Get lyrics for a track
     * GET /v4/lyrics?title={title}&artist={artist}
     */
    @GetMapping("/v4/lyrics")
    public ResponseEntity<Map<String, Object>> getLyrics(
            @RequestParam String title,
            @RequestParam String artist) {
        
        if (!config.getLyrics().isEnabled()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Lyrics feature is disabled"
            ));
        }

        log.info("Fetching lyrics for: {} - {}", artist, title);
        
        LyricsService.LyricsResult result = lyricsService.getLyrics(title, artist);
        
        if (result == null || !result.hasLyrics()) {
            return ResponseEntity.ok(Map.of(
                "title", title,
                "artist", artist,
                "lyrics", "",
                "found", false
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("title", title);
        response.put("artist", artist);
        response.put("lyrics", result.getLyrics());
        response.put("provider", result.getProvider());
        response.put("found", true);
        response.put("lines", result.getLines());

        return ResponseEntity.ok(response);
    }

    /**
     * Search for lyrics
     * GET /v4/lyrics/search?query={query}
     */
    @GetMapping("/v4/lyrics/search")
    public ResponseEntity<Map<String, Object>> searchLyrics(@RequestParam String query) {
        if (!config.getLyrics().isEnabled()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Lyrics feature is disabled"
            ));
        }

        // Basic search implementation - split query into artist and title
        String[] parts = query.split(" - ", 2);
        String artist = parts.length > 1 ? parts[0] : "";
        String title = parts.length > 1 ? parts[1] : query;

        LyricsService.LyricsResult result = lyricsService.getLyrics(title, artist);
        
        if (result == null || !result.hasLyrics()) {
            return ResponseEntity.ok(Map.of(
                "query", query,
                "results", List.of(),
                "found", false
            ));
        }

        return ResponseEntity.ok(Map.of(
            "query", query,
            "results", List.of(Map.of(
                "title", title,
                "artist", artist,
                "provider", result.getProvider()
            )),
            "found", true
        ));
    }

    /**
     * Get lyrics cache statistics
     * GET /v4/lyrics/stats
     */
    @GetMapping("/v4/lyrics/stats")
    public ResponseEntity<Map<String, Object>> getLyricsStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", lyricsService.getCacheSize());
        stats.put("enabled", config.getLyrics().isEnabled());
        stats.put("providers", config.getLyrics().getProviders());
        stats.put("cacheEnabled", config.getLyrics().isCacheEnabled());
        stats.put("cacheDurationMinutes", config.getLyrics().getCacheDurationMinutes());

        return ResponseEntity.ok(stats);
    }

    /**
     * Clear lyrics cache
     * DELETE /v4/lyrics/cache
     */
    @DeleteMapping("/v4/lyrics/cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        lyricsService.clearCache();
        log.info("Lyrics cache cleared");
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Lyrics cache cleared"
        ));
    }

    /**
     * Health check endpoint
     * GET /v4/lyrics/health
     */
    @GetMapping("/v4/lyrics/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "enabled", config.getLyrics().isEnabled(),
            "service", "lyrics"
        ));
    }

    @Override
    public @NotNull String getRoute() {
        return "/v4/lyrics";
    }
}

