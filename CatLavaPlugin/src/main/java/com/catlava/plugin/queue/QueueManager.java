package com.catlava.plugin.queue;

import com.catlava.plugin.config.PluginConfig;
import com.catlava.plugin.event.EventHandlers;
import dev.arbjerg.lavalink.api.audio.Track;
import dev.arbjerg.lavalink.api.audio.TrackEndReason;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Manages music queues for multiple guilds.
 * Supports various queue modes including repeat, shuffle, and track history.
 */
@Component
public class QueueManager {

    private static final Logger log = LoggerFactory.getLogger(QueueManager.class);

    private final PluginConfig config;
    private final Map<String, GuildQueue> queues = new ConcurrentHashMap<>();

    public QueueManager(PluginConfig config) {
        this.config = config;
    }

    /**
     * Get or create a queue for a guild
     */
    public GuildQueue getQueue(String guildId) {
        return queues.computeIfAbsent(guildId, k -> new GuildQueue(guildId, config));
    }

    /**
     * Add a track to the queue
     */
    public void addTrack(String guildId, Track track) {
        getQueue(guildId).addTrack(track);
    }

    /**
     * Add multiple tracks to the queue
     */
    public void addTracks(String guildId, Collection<Track> tracks) {
        getQueue(guildId).addTracks(tracks);
    }

    /**
     * Get the next track in queue
     */
    public Track nextTrack(String guildId) {
        return getQueue(guildId).nextTrack();
    }

    /**
     * Get the current queue size
     */
    public int getQueueSize(String guildId) {
        return getQueue(guildId).size();
    }

    /**
     * Clear the queue
     */
    public void clearQueue(String guildId) {
        getQueue(guildId).clear();
    }

    /**
     * Shuffle the queue
     */
    public void shuffleQueue(String guildId) {
        getQueue(guildId).shuffle();
    }

    /**
     * Remove a specific track from queue
     */
    public boolean removeTrack(String guildId, int index) {
        return getQueue(guildId).removeTrack(index);
    }

    /**
     * Set repeat mode
     */
    public void setRepeatMode(String guildId, RepeatMode mode) {
        getQueue(guildId).setRepeatMode(mode);
    }

    /**
     * Get repeat mode
     */
    public RepeatMode getRepeatMode(String guildId) {
        return getQueue(guildId).getRepeatMode();
    }

    /**
     * Get queue history
     */
    public List<Track> getHistory(String guildId) {
        return getQueue(guildId).getHistory();
    }

    // ==================== Event Callbacks ====================

    public void onTrackStart(String guildId, Track track) {
        GuildQueue queue = queues.get(guildId);
        if (queue != null) {
            queue.onTrackStart(track);
        }
    }

    public void onTrackEnd(String guildId, Track track, TrackEndReason endReason) {
        GuildQueue queue = queues.get(guildId);
        if (queue != null) {
            queue.onTrackEnd(track, endReason);
        }
    }

    public void onTrackException(String guildId, Track track, String exception) {
        log.warn("Track exception in guild {}: {}", guildId, exception);
    }

    public void onTrackStuck(String guildId, Track track, long thresholdMs) {
        log.warn("Track stuck in guild {}: {}ms", guildId, thresholdMs);
    }

    public void onPlayerPause(String guildId) {
        GuildQueue queue = queues.get(guildId);
        if (queue != null) {
            queue.setPaused(true);
        }
    }

    public void onPlayerResume(String guildId) {
        GuildQueue queue = queues.get(guildId);
        if (queue != null) {
            queue.setPaused(false);
        }
    }

    public void onPlayerDestroy(String guildId) {
        // Optionally cleanup queue on player destroy
        // queues.remove(guildId);
    }

    /**
     * Get all queues (for stats)
     */
    public Map<String, GuildQueue> getAllQueues() {
        return Collections.unmodifiableMap(queues);
    }

    /**
     * Remove empty queues
     */
    public void cleanup() {
        queues.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    // ==================== Repeat Mode Enum ====================

    public enum RepeatMode {
        OFF,
        ALL,
        SINGLE
    }

    // ==================== Guild Queue Implementation ====================

    public static class GuildQueue {
        private final String guildId;
        private final PluginConfig config;
        private final Deque<Track> queue = new ConcurrentLinkedDeque<>();
        private final Deque<Track> history = new ConcurrentLinkedDeque<>();
        private final Random random = new Random();

        private Track currentTrack;
        private RepeatMode repeatMode = RepeatMode.OFF;
        private boolean paused = false;
        private int defaultVolume = 100;

        public GuildQueue(String guildId, PluginConfig config) {
            this.guildId = guildId;
            this.config = config;
            this.defaultVolume = config.getQueue().getDefaultVolume();
        }

        public void addTrack(Track track) {
            if (queue.size() >= config.getQueue().getMaxSize()) {
                log.warn("Queue full for guild {}", guildId);
                return;
            }
            queue.addLast(track);
            log.debug("Track added to queue for guild {}: {}", guildId, track.getInfo().getTitle());
        }

        public void addTracks(Collection<Track> tracks) {
            for (Track track : tracks) {
                addTrack(track);
            }
        }

        public Track nextTrack() {
            Track track = queue.pollFirst();
            if (track != null) {
                // Handle repeat mode
                if (repeatMode == RepeatMode.ALL) {
                    queue.addLast(track);
                } else if (repeatMode == RepeatMode.SINGLE && currentTrack != null) {
                    queue.addFirst(currentTrack);
                }
            }
            return track;
        }

        public int size() {
            return queue.size();
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }

        public void clear() {
            queue.clear();
            log.debug("Queue cleared for guild {}", guildId);
        }

        public void shuffle() {
            List<Track> list = new ArrayList<>(queue);
            Collections.shuffle(list, random);
            queue.clear();
            queue.addAll(list);
            log.debug("Queue shuffled for guild {}", guildId);
        }

        public boolean removeTrack(int index) {
            if (index < 0 || index >= queue.size()) {
                return false;
            }
            List<Track> list = new ArrayList<>(queue);
            Track removed = list.remove(index);
            queue.clear();
            queue.addAll(list);
            log.debug("Track removed from queue for guild {}: {}", guildId, removed.getInfo().getTitle());
            return true;
        }

        public void setRepeatMode(RepeatMode mode) {
            this.repeatMode = mode;
            log.debug("Repeat mode set for guild {}: {}", guildId, mode);
        }

        public RepeatMode getRepeatMode() {
            return repeatMode;
        }

        public List<Track> getHistory() {
            return new ArrayList<>(history);
        }

        public void onTrackStart(Track track) {
            this.currentTrack = track;
            this.paused = false;
            log.debug("Track started in guild {}: {}", guildId, track.getInfo().getTitle());
        }

        public void onTrackEnd(Track track, TrackEndReason endReason) {
            // Add to history
            history.addFirst(track);
            
            // Keep history limited
            while (history.size() > 100) {
                history.removeLast();
            }

            if (!endReason.mayPlayNext()) {
                // Track won't play again naturally (e.g., stopped, replaced)
                this.currentTrack = null;
            }
            
            log.debug("Track ended in guild {}: {}", guildId, endReason.name());
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
        }

        public boolean isPaused() {
            return paused;
        }

        public Track getCurrentTrack() {
            return currentTrack;
        }

        public int getDefaultVolume() {
            return defaultVolume;
        }

        public void setDefaultVolume(int defaultVolume) {
            this.defaultVolume = defaultVolume;
        }

        public List<Track> getQueueAsList() {
            return new ArrayList<>(queue);
        }
    }
}

