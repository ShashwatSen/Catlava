package com.catlava.plugin.audio;

import com.catlava.plugin.config.PluginConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for fetching lyrics from various providers.
 * Supports caching and multiple lyrics sources.
 */
@Component
public class LyricsService {

    private static final Logger log = LoggerFactory.getLogger(LyricsService.class);
    
    private final PluginConfig config;
    private final HttpClient httpClient;
    private final Map<String, CachedLyrics> lyricsCache = new ConcurrentHashMap<>();

    public LyricsService(PluginConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Search for lyrics by track title and artist
     */
    @Nullable
    public LyricsResult getLyrics(@NotNull String title, @NotNull String artist) {
        if (!config.getLyrics().isEnabled()) {
            log.debug("Lyrics feature is disabled");
            return null;
        }

        String cacheKey = createCacheKey(title, artist);
        
        // Check cache first
        CachedLyrics cached = lyricsCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached lyrics for: {} - {}", artist, title);
            return cached.getResult();
        }

        // Try each provider
        for (String provider : config.getLyrics().getProviders()) {
            try {
                LyricsResult result = fetchLyrics(title, artist, provider);
                if (result != null && result.hasLyrics()) {
                    // Cache the result
                    cacheLyrics(cacheKey, result);
                    return result;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch lyrics from {}: {}", provider, e.getMessage());
            }
        }

        log.info("No lyrics found for: {} - {}", artist, title);
        return null;
    }

    /**
     * Fetch lyrics from a specific provider
     */
    @Nullable
    private LyricsResult fetchLyrics(@NotNull String title, @NotNull String artist, @NotNull String provider) {
        String url = buildSearchUrl(title, artist, provider);
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "CatLava/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseLyricsResponse(response.body(), provider);
            }
            
            log.debug("Lyrics provider {} returned status {}", provider, response.statusCode());
            
        } catch (IOException | InterruptedException e) {
            log.error("Error fetching lyrics from {}: {}", provider, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        
        return null;
    }

    @NotNull
    private String buildSearchUrl(@NotNull String title, @NotNull String artist, @NotNull String provider) {
        String encodedTitle = title.replace(" ", "+");
        String encodedArtist = artist.replace(" ", "+");
        
        return switch (provider.toLowerCase()) {
            case "lyrics" -> String.format(
                "https://api.lyrics.ovh/v1/%s/%s",
                encodedArtist, encodedTitle
            );
            case "genius" -> String.format(
                "https://api.genius.com/search?q=%s%%20%s",
                encodedArtist, encodedTitle
            );
            default -> String.format(
                "https://api.lyrics.ovh/v1/%s/%s",
                encodedArtist, encodedTitle
            );
        };
    }

    @Nullable
    private LyricsResult parseLyricsResponse(@NotNull String body, @NotNull String provider) {
        try {
            // Simple parsing for lyrics.ovh format
            if (provider.equals("lyrics") || body.contains("lyrics")) {
                // Parse JSON response
                String lyrics = extractJsonValue(body, "lyrics");
                if (lyrics != null && !lyrics.isEmpty()) {
                    return new LyricsResult(lyrics, provider, false);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse lyrics response: {}", e.getMessage());
        }
        
        return null;
    }

    @Nullable
    private String extractJsonValue(@NotNull String json, @NotNull String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int startQuote = json.indexOf("\"", colonIndex);
        int endQuote = json.indexOf("\"", startQuote + 1);
        
        if (startQuote == -1 || endQuote == -1) return null;
        
        return json.substring(startQuote + 1, endQuote);
    }

    private void cacheLyrics(@NotNull String key, @NotNull LyricsResult result) {
        if (config.getLyrics().isCacheEnabled()) {
            long duration = config.getLyrics().getCacheDurationMinutes() * 60 * 1000L;
            lyricsCache.put(key, new CachedLyrics(result, duration));
            log.debug("Cached lyrics for key: {}", key);
        }
    }

    @NotNull
    private String createCacheKey(@NotNull String title, @NotNull String artist) {
        return (artist + "-" + title).toLowerCase().replace(" ", "_");
    }

    /**
     * Clear the lyrics cache
     */
    public void clearCache() {
        lyricsCache.clear();
        log.info("Lyrics cache cleared");
    }

    /**
     * Get cache size
     */
    public int getCacheSize() {
        return lyricsCache.size();
    }

    // ==================== Inner Classes ====================

    /**
     * Represents lyrics data
     */
    public static class LyricsResult {
        private final String lyrics;
        private final String provider;
        private final boolean instrumental;

        public LyricsResult(String lyrics, String provider, boolean instrumental) {
            this.lyrics = lyrics;
            this.provider = provider;
            this.instrumental = instrumental;
        }

        public String getLyrics() {
            return lyrics;
        }

        public String getProvider() {
            return provider;
        }

        public boolean isInstrumental() {
            return instrumental;
        }

        public boolean hasLyrics() {
            return lyrics != null && !lyrics.isEmpty() && !instrumental;
        }

        /**
         * Get lyrics as lines
         */
        public List<String> getLines() {
            if (lyrics == null) return List.of();
            return List.of(lyrics.split("\n"));
        }
    }

    /**
     * Cached lyrics with expiration
     */
    private static class CachedLyrics {
        private final LyricsResult result;
        private final long expiryTime;

        public CachedLyrics(LyricsResult result, long durationMs) {
            this.result = result;
            this.expiryTime = System.currentTimeMillis() + durationMs;
        }

        public LyricsResult getResult() {
            return result;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}

