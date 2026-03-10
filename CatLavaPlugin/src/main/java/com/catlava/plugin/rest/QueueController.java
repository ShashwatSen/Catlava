package com.catlava.plugin.rest;

import com.catlava.plugin.config.PluginConfig;
import com.catlava.plugin.queue.QueueManager;
import dev.arbjerg.lavalink.api.RestController;
import dev.arbjerg.lavalink.api.audio.Track;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for queue management endpoints.
 * Provides API endpoints for managing music queues.
 */
@RestController
public class QueueController implements RestController {

    private static final Logger log = LoggerFactory.getLogger(QueueController.class);
    
    private final QueueManager queueManager;
    private final PluginConfig config;

    public QueueController(QueueManager queueManager, PluginConfig config) {
        this.queueManager = queueManager;
        this.config = config;
    }

    /**
     * Get queue for a guild
     * GET /v4/queue/{guildId}
     */
    @GetMapping("/v4/queue/{guildId}")
    public ResponseEntity<Map<String, Object>> getQueue(@PathVariable String guildId) {
        QueueManager.GuildQueue queue = queueManager.getQueue(guildId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("guildId", guildId);
        response.put("size", queue.size());
        response.put("repeatMode", queue.getRepeatMode().name());
        response.put("paused", queue.isPaused());
        
        // Add current track info
        Track currentTrack = queue.getCurrentTrack();
        if (currentTrack != null) {
            response.put("currentTrack", formatTrackInfo(currentTrack));
        }
        
        // Add queue tracks
        List<Map<String, Object>> tracks = new ArrayList<>();
        for (Track track : queue.getQueueAsList()) {
            tracks.add(formatTrackInfo(track));
        }
        response.put("tracks", tracks);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get queue history for a guild
     * GET /v4/queue/{guildId}/history
     */
    @GetMapping("/v4/queue/{guildId}/history")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable String guildId) {
        List<Track> history = queueManager.getHistory(guildId);
        
        List<Map<String, Object>> tracks = new ArrayList<>();
        for (Track track : history) {
            tracks.add(formatTrackInfo(track));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("guildId", guildId);
        response.put("historySize", history.size());
        response.put("tracks", tracks);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Clear queue for a guild
     * DELETE /v4/queue/{guildId}
     */
    @DeleteMapping("/v4/queue/{guildId}")
    public ResponseEntity<Map<String, Object>> clearQueue(@PathVariable String guildId) {
        queueManager.clearQueue(guildId);
        log.info("Queue cleared for guild: {}", guildId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "guildId", guildId,
            "message", "Queue cleared"
        ));
    }

    /**
     * Shuffle queue for a guild
     * POST /v4/queue/{guildId}/shuffle
     */
    @PostMapping("/v4/queue/{guildId}/shuffle")
    public ResponseEntity<Map<String, Object>> shuffleQueue(@PathVariable String guildId) {
        queueManager.shuffleQueue(guildId);
        log.info("Queue shuffled for guild: {}", guildId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "guildId", guildId,
            "message", "Queue shuffled"
        ));
    }

    /**
     * Set repeat mode for a guild
     * POST /v4/queue/{guildId}/repeat
     */
    @PostMapping("/v4/queue/{guildId}/repeat")
    public ResponseEntity<Map<String, Object>> setRepeatMode(
            @PathVariable String guildId,
            @RequestParam String mode) {
        
        try {
            QueueManager.RepeatMode repeatMode = QueueManager.RepeatMode.valueOf(mode.toUpperCase());
            queueManager.setRepeatMode(guildId, repeatMode);
            log.info("Repeat mode set to {} for guild: {}", repeatMode, guildId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "guildId", guildId,
                "repeatMode", repeatMode.name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid repeat mode. Valid values: OFF, ALL, SINGLE"
            ));
        }
    }

    /**
     * Remove track from queue
     * DELETE /v4/queue/{guildId}/track/{index}
     */
    @DeleteMapping("/v4/queue/{guildId}/track/{index}")
    public ResponseEntity<Map<String, Object>> removeTrack(
            @PathVariable String guildId,
            @PathVariable int index) {
        
        boolean removed = queueManager.removeTrack(guildId, index);
        
        if (removed) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "guildId", guildId,
                "removedIndex", index,
                "message", "Track removed from queue"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid track index"
            ));
        }
    }

    /**
     * Get queue statistics
     * GET /v4/queue/stats
     */
    @GetMapping("/v4/queue/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, QueueManager.GuildQueue> queues = queueManager.getAllQueues();
        
        int totalQueues = queues.size();
        int totalTracks = 0;
        int activePlayers = 0;
        
        for (QueueManager.GuildQueue queue : queues.values()) {
            totalTracks += queue.size();
            if (!queue.isEmpty() || queue.getCurrentTrack() != null) {
                activePlayers++;
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalQueues", totalQueues);
        response.put("totalTracks", totalTracks);
        response.put("activePlayers", activePlayers);
        response.put("maxQueueSize", config.getQueue().getMaxSize());
        response.put("defaultVolume", config.getQueue().getDefaultVolume());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get available queue info for all guilds
     * GET /v4/queue/all
     */
    @GetMapping("/v4/queue/all")
    public ResponseEntity<Map<String, Object>> getAllQueues() {
        Map<String, QueueManager.GuildQueue> queues = queueManager.getAllQueues();
        
        Map<String, Map<String, Object>> guildQueues = new HashMap<>();
        for (Map.Entry<String, QueueManager.GuildQueue> entry : queues.entrySet()) {
            QueueManager.GuildQueue queue = entry.getValue();
            Map<String, Object> queueInfo = new HashMap<>();
            queueInfo.put("size", queue.size());
            queueInfo.put("repeatMode", queue.getRepeatMode().name());
            queueInfo.put("hasCurrentTrack", queue.getCurrentTrack() != null);
            guildQueues.put(entry.getKey(), queueInfo);
        }
        
        return ResponseEntity.ok(Map.of(
            "queues", guildQueues,
            "totalGuilds", guildQueues.size()
        ));
    }

    /**
     * Health check endpoint
     * GET /v4/queue/health
     */
    @GetMapping("/v4/queue/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "queue"
        ));
    }

    private Map<String, Object> formatTrackInfo(Track track) {
        Map<String, Object> info = new HashMap<>();
        info.put("title", track.getInfo().getTitle());
        info.put("author", track.getInfo().getAuthor());
        info.put("uri", track.getInfo().getUri());
        info.put("length", track.getInfo().getLength());
        info.put("identifier", track.getInfo().getIdentifier());
        info.put("isSeekable", track.getInfo().isSeekable());
        return info;
    }

    @Override
    public @NotNull String getRoute() {
        return "/v4/queue";
    }
}

