package com.catlava.plugin.audio;

import com.catlava.plugin.config.PluginConfig;
import dev.arbjerg.lavalink.api.PlayerContext;
import dev.arbjerg.lavalink.api.audio.Filters;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Custom audio filters and equalizer presets.
 * Provides predefined and custom filter configurations.
 */
@Component
public class CustomFilters {

    private static final Logger log = LoggerFactory.getLogger(CustomFilters.class);
    
    private final PluginConfig config;

    public CustomFilters(PluginConfig config) {
        this.config = config;
    }

    /**
     * Apply an equalizer preset by name
     */
    public boolean applyEqualizerPreset(@NotNull PlayerContext context, @NotNull String presetName) {
        Map<String, List<PluginConfig.EqualizerBand>> presets = config.getFilterPresets();
        
        List<PluginConfig.EqualizerBand> preset = presets.get(presetName);
        if (preset == null) {
            log.warn("Equalizer preset not found: {}", presetName);
            return false;
        }

        try {
            Filters filters = context.getFilters();
            
            // Build equalizer bands
            Filters.EqualizerBand[] bands = new Filters.EqualizerBand[15]; // Standard 15-band EQ
            for (int i = 0; i < 15; i++) {
                bands[i] = new Filters.EqualizerBand(i, 0.0f);
            }
            
            // Apply preset gains
            for (PluginConfig.EqualizerBand band : preset) {
                if (band.getBand() >= 0 && band.getBand() < 15) {
                    bands[band.getBand()] = new Filters.EqualizerBand(band.getBand(), band.getGain());
                }
            }
            
            filters.setEqualizer(bands);
            log.info("Applied equalizer preset: {} to guild: {}", presetName, context.getGuildId());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to apply equalizer preset: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Apply bass boost effect
     */
    public void applyBassBoost(@NotNull PlayerContext context) {
        applyEqualizerPreset(context, "bass_boost");
    }

    /**
     * Apply treble boost effect
     */
    public void applyTrebleBoost(@NotNull PlayerContext context) {
        applyEqualizerPreset(context, "treble_boost");
    }

    /**
     * Reset all filters
     */
    public void resetFilters(@NotNull PlayerContext context) {
        try {
            Filters filters = context.getFilters();
            filters.setEqualizer(new Filters.EqualizerBand[0]);
            filters.setVolume(1.0f);
            filters.setKaraoke(null);
            filters.setTimescale(null);
            filters.setTremolo(null);
            filters.setVibrato(null);
            filters.setDistortion(null);
            filters.setRotation(null);
            filters.setChannelMix(null);
            filters.setLowPass(null);
            log.info("Reset all filters for guild: {}", context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to reset filters: {}", e.getMessage());
        }
    }

    /**
     * Apply volume filter
     */
    public void setVolume(@NotNull PlayerContext context, float volume) {
        try {
            Filters filters = context.getFilters();
            filters.setVolume(Math.max(0.0f, Math.min(5.0f, volume)));
            log.debug("Set volume to {} for guild: {}", volume, context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to set volume: {}", e.getMessage());
        }
    }

    /**
     * Apply karaoke filter
     */
    public void applyKaraoke(@NotNull PlayerContext context, float level, float monoLevel, float filterBand, float filterWidth) {
        try {
            Filters filters = context.getFilters();
            filters.setKaraoke(new Filters.Karaoke(
                Math.max(0.0f, Math.min(1.0f, level)),
                Math.max(0.0f, Math.min(1.0f, monoLevel)),
                new Filters.Karaoke.KaraokeLevelFilter(
                    Math.max(0.0f, Math.min(1.0f, filterBand)),
                    Math.max(0.0f, Math.min(1.0f, filterWidth))
                )
            ));
            log.debug("Applied karaoke filter to guild: {}", context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to apply karaoke filter: {}", e.getMessage());
        }
    }

    /**
     * Apply timescale (speed/pitch) effect
     */
    public void applyTimescale(@NotNull PlayerContext context, float speed, float pitch, float rate) {
        try {
            Filters filters = context.getFilters();
            filters.setTimescale(new Filters.Timescale(
                Math.max(0.0f, Math.min(10.0f, speed)),
                Math.max(0.0f, Math.min(10.0f, pitch)),
                Math.max(0.0f, Math.min(10.0f, rate))
            ));
            log.debug("Applied timescale effect to guild: {}", context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to apply timescale effect: {}", e.getMessage());
        }
    }

    /**
     * Apply tremolo effect
     */
    public void applyTremolo(@NotNull PlayerContext context, float frequency, float depth) {
        try {
            Filters filters = context.getFilters();
            filters.setTremolo(new Filters.Tremolo(
                Math.max(0.1f, Math.min(14.0f, frequency)),
                Math.max(0.0f, Math.min(1.0f, depth))
            ));
            log.debug("Applied tremolo effect to guild: {}", context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to apply tremolo effect: {}", e.getMessage());
        }
    }

    /**
     * Apply vibrato effect
     */
    public void applyVibrato(@NotNull PlayerContext context, float frequency, float depth) {
        try {
            Filters filters = context.getFilters();
            filters.setVibrato(new Filters.Vibrato(
                Math.max(0.1f, Math.min(14.0f, frequency)),
                Math.max(0.0f, Math.min(1.0f, depth))
            ));
            log.debug("Applied vibrato effect to guild: {}", context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to apply vibrato effect: {}", e.getMessage());
        }
    }

    /**
     * Apply distortion effect
     */
    public void applyDistortion(@NotNull PlayerContext context, float sinOffset, float sinScale, float cosOffset, float cosScale, float tanOffset, float tanScale, float offset, float scale) {
        try {
            Filters filters = context.getFilters();
            filters.setDistortion(new Filters.Distortion(sinOffset, sinScale, cosOffset, cosScale, tanOffset, tanScale, offset, scale));
            log.debug("Applied distortion effect to guild: {}", context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to apply distortion effect: {}", e.getMessage());
        }
    }

    /**
     * Apply rotation effect
     */
    public void applyRotation(@NotNull PlayerContext context, float hz) {
        try {
            Filters filters = context.getFilters();
            filters.setRotation(new Filters.Rotation(hz));
            log.debug("Applied rotation effect to guild: {}", context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to apply rotation effect: {}", e.getMessage());
        }
    }

    /**
     * Apply channel mix
     */
    public void applyChannelMix(@NotNull PlayerContext context, float leftToLeft, float leftToRight, float rightToLeft, float rightToRight) {
        try {
            Filters filters = context.getFilters();
            filters.setChannelMix(new Filters.ChannelMix(leftToLeft, leftToRight, rightToLeft, rightToRight));
            log.debug("Applied channel mix to guild: {}", context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to apply channel mix: {}", e.getMessage());
        }
    }

    /**
     * Apply low pass filter
     */
    public void applyLowPass(@NotNull PlayerContext context, float smoothing) {
        try {
            Filters filters = context.getFilters();
            filters.setLowPass(new Filters.LowPass(Math.max(0.0f, Math.min(1.0f, smoothing))));
            log.debug("Applied low pass filter to guild: {}", context.getGuildId());
        } catch (Exception e) {
            log.error("Failed to apply low pass filter: {}", e.getMessage());
        }
    }

    /**
     * Get available preset names
     */
    public List<String> getAvailablePresets() {
        return List.copyOf(config.getFilterPresets().keySet());
    }

    /**
     * Check if a preset exists
     */
    public boolean hasPreset(@NotNull String presetName) {
        return config.getFilterPresets().containsKey(presetName);
    }
}

