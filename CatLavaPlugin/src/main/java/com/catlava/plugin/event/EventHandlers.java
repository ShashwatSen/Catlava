package com.catlava.plugin.event;

import com.catlava.plugin.config.PluginConfig;
import com.catlava.plugin.queue.QueueManager;
import dev.arbjerg.lavalink.api.ISocketContext;
import dev.arbjerg.lavalink.api.PluginEventHandler;
import dev.arbjerg.lavalink.api.audio.AudioPlayerExtended;
import dev.arbjerg.lavalink.api.audio.Track;
import dev.arbjerg.lavalink.api.audio.VoiceState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event handlers for various LavaLink events.
 * Handles track events, player events, and WebSocket events.
 */
@Component
public class EventHandlers extends PluginEventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventHandlers.class);
    
    private final PluginConfig config;
    private final QueueManager queueManager;

    public EventHandlers(PluginConfig config, QueueManager queueManager) {
        this.config = config;
        this.queueManager = queueManager;
        log.info("CatLava EventHandlers initialized");
    }

    // ==================== WebSocket Events ====================

    @Override
    public void onWebSocketOpen(@NotNull ISocketContext context, boolean resumed) {
        String sessionId = context.getSessionId();
        log.info("WebSocket opened! Session: {}, Resumed: {}", sessionId, resumed);
        
        Map<String, Object> eventData = createEventData("websocket_open");
        eventData.put("sessionId", sessionId);
        eventData.put("resumed", resumed);
        eventData.put("guildId", context.getGuildId());
        
        logEvent(eventData);
    }

    @Override
    public void onWebSocketClosed(int closeCode, @NotNull String reason, boolean byRemote, @NotNull ISocketContext context) {
        String sessionId = context.getSessionId();
        log.info("WebSocket closed! Session: {}, Code: {}, Reason: {}, ByRemote: {}", 
                sessionId, closeCode, reason, byRemote);
        
        Map<String, Object> eventData = createEventData("websocket_closed");
        eventData.put("sessionId", sessionId);
        eventData.put("closeCode", closeCode);
        eventData.put("reason", reason);
        eventData.put("byRemote", byRemote);
        eventData.put("guildId", context.getGuildId());
        
        logEvent(eventData);
    }

    // ==================== Track Events ====================

    @Override
    public void onTrackStart(@NotNull ISocketContext context, @NotNull Track track) {
        String guildId = context.getGuildId();
        log.info("Track started! Guild: {}, Track: {}", guildId, track.getInfo().getTitle());
        
        Map<String, Object> eventData = createEventData("track_start");
        eventData.put("guildId", guildId);
        eventData.put("trackTitle", track.getInfo().getTitle());
        eventData.put("trackAuthor", track.getInfo().getAuthor());
        eventData.put("trackUri", track.getInfo().getUri());
        eventData.put("trackLength", track.getInfo().getLength());
        eventData.put("trackIdentifier", track.getInfo().getIdentifier());
        
        logEvent(eventData);

        // Update queue manager
        queueManager.onTrackStart(guildId, track);
    }

    @Override
    public void onTrackEnd(@NotNull ISocketContext context, @NotNull Track track, @NotNull TrackEndReason endReason) {
        String guildId = context.getGuildId();
        log.info("Track ended! Guild: {}, Track: {}, Reason: {}", 
                guildId, track.getInfo().getTitle(), endReason.name());
        
        Map<String, Object> eventData = createEventData("track_end");
        eventData.put("guildId", guildId);
        eventData.put("trackTitle", track.getInfo().getTitle());
        eventData.put("endReason", endReason.name());
        eventData.put("mayPlayNext", endReason.mayPlayNext());
        
        logEvent(eventData);

        // Update queue manager
        queueManager.onTrackEnd(guildId, track, endReason);
    }

    @Override
    public void onTrackException(@NotNull ISocketContext context, @NotNull Track track, @NotNull String exception) {
        String guildId = context.getGuildId();
        log.error("Track exception! Guild: {}, Track: {}, Exception: {}", 
                guildId, track.getInfo().getTitle(), exception);
        
        Map<String, Object> eventData = createEventData("track_exception");
        eventData.put("guildId", guildId);
        eventData.put("trackTitle", track.getInfo().getTitle());
        eventData.put("exception", exception);
        
        logEvent(eventData);

        // Update queue manager
        queueManager.onTrackException(guildId, track, exception);
    }

    @Override
    public void onTrackStuck(@NotNull ISocketContext context, @NotNull Track track, long thresholdMs) {
        String guildId = context.getGuildId();
        log.warn("Track stuck! Guild: {}, Track: {}, Threshold: {}ms", 
                guildId, track.getInfo().getTitle(), thresholdMs);
        
        Map<String, Object> eventData = createEventData("track_stuck");
        eventData.put("guildId", guildId);
        eventData.put("trackTitle", track.getInfo().getTitle());
        eventData.put("thresholdMs", thresholdMs);
        
        logEvent(eventData);

        // Update queue manager
        queueManager.onTrackStuck(guildId, track, thresholdMs);
    }

    // ==================== Player Events ====================

    @Override
    public void onPlayerPause(@NotNull ISocketContext context) {
        String guildId = context.getGuildId();
        log.info("Player paused! Guild: {}", guildId);
        
        Map<String, Object> eventData = createEventData("player_pause");
        eventData.put("guildId", guildId);
        
        logEvent(eventData);

        queueManager.onPlayerPause(guildId);
    }

    @Override
    public void onPlayerResume(@NotNull ISocketContext context) {
        String guildId = context.getGuildId();
        log.info("Player resumed! Guild: {}", guildId);
        
        Map<String, Object> eventData = createEventData("player_resume");
        eventData.put("guildId", guildId);
        
        logEvent(eventData);

        queueManager.onPlayerResume(guildId);
    }

    @Override
    public void onPlayerDestroy(@NotNull ISocketContext context) {
        String guildId = context.getGuildId();
        log.info("Player destroyed! Guild: {}", guildId);
        
        Map<String, Object> eventData = createEventData("player_destroy");
        eventData.put("guildId", guildId);
        
        logEvent(eventData);

        queueManager.onPlayerDestroy(guildId);
    }

    // ==================== Voice Events ====================

    @Override
    public void onVoiceStateUpdate(@NotNull ISocketContext context, @NotNull VoiceState voiceState) {
        String guildId = context.getGuildId();
        
        if (log.isDebugEnabled()) {
            log.debug("Voice state update! Guild: {}, ChannelId: {}, UserId: {}, SessionId: {}", 
                    guildId, voiceState.getChannelId(), voiceState.getUserId(), voiceState.getSessionId());
        }
        
        Map<String, Object> eventData = createEventData("voice_state_update");
        eventData.put("guildId", guildId);
        eventData.put("channelId", voiceState.getChannelId());
        eventData.put("userId", voiceState.getUserId());
        eventData.put("sessionId", voiceState.getSessionId());
        eventData.put("suppressed", voiceState.isSuppressed());
        eventData.put("deafened", voiceState.isDeafened());
        eventData.put("muted", voiceState.isMuted());
        
        logEvent(eventData);
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> createEventData(String eventType) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventType", eventType);
        data.put("timestamp", Instant.now().toEpochMilli());
        return data;
    }

    private void logEvent(Map<String, Object> eventData) {
        if (!config.getLogging().isEnabled()) {
            return;
        }
        
        // Format event data as string
        StringBuilder sb = new StringBuilder();
        sb.append("Event[");
        eventData.forEach((key, value) -> {
            sb.append(key).append("=").append(value).append(", ");
        });
        if (sb.length() > 7) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("]");
        
        log.info(sb.toString());
    }
}

