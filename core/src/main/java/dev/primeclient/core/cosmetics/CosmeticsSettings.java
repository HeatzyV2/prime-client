package dev.primeclient.core.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.primeclient.core.config.ConfigBinding;

/** Performance and visibility settings for cosmetics particles / peers. */
public final class CosmeticsSettings implements ConfigBinding {

    public enum ParticlesQuality {
        OFF,
        LOW,
        MED,
        HIGH
    }

    private boolean enabled = true;
    private ParticlesQuality particlesQuality = ParticlesQuality.MED;
    private boolean showOthers = true;
    private boolean showOwn = true;
    private double maxDistance = 48.0;
    private boolean autoDowngrade = true;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ParticlesQuality particlesQuality() {
        return particlesQuality;
    }

    public void setParticlesQuality(ParticlesQuality quality) {
        this.particlesQuality = quality != null ? quality : ParticlesQuality.MED;
    }

    public boolean showOthers() {
        return showOthers;
    }

    public void setShowOthers(boolean showOthers) {
        this.showOthers = showOthers;
    }

    public boolean showOwn() {
        return showOwn;
    }

    public void setShowOwn(boolean showOwn) {
        this.showOwn = showOwn;
    }

    public double maxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = Math.max(8.0, Math.min(128.0, maxDistance));
    }

    public boolean autoDowngrade() {
        return autoDowngrade;
    }

    public void setAutoDowngrade(boolean autoDowngrade) {
        this.autoDowngrade = autoDowngrade;
    }

    /** Effective particle budget (0 = off) after optional FPS auto-downgrade. */
    public int particleBudget(float fps) {
        ParticlesQuality q = particlesQuality;
        if (!enabled || q == ParticlesQuality.OFF) {
            return 0;
        }
        if (autoDowngrade && fps > 0 && fps < 40) {
            q = ParticlesQuality.LOW;
        }
        if (autoDowngrade && fps > 0 && fps < 25) {
            return 0;
        }
        return switch (q) {
            case OFF -> 0;
            case LOW -> 8;
            case MED -> 24;
            case HIGH -> 48;
        };
    }

    @Override
    public String configKey() {
        return "cosmetics_settings";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("particlesQuality", particlesQuality.name());
        json.addProperty("showOthers", showOthers);
        json.addProperty("showOwn", showOwn);
        json.addProperty("maxDistance", maxDistance);
        json.addProperty("autoDowngrade", autoDowngrade);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("enabled")) {
            enabled = json.get("enabled").getAsBoolean();
        }
        if (json.has("particlesQuality")) {
            try {
                particlesQuality = ParticlesQuality.valueOf(json.get("particlesQuality").getAsString());
            } catch (IllegalArgumentException ignored) {
                particlesQuality = ParticlesQuality.MED;
            }
        }
        if (json.has("showOthers")) {
            showOthers = json.get("showOthers").getAsBoolean();
        }
        if (json.has("showOwn")) {
            showOwn = json.get("showOwn").getAsBoolean();
        }
        if (json.has("maxDistance")) {
            setMaxDistance(json.get("maxDistance").getAsDouble());
        }
        if (json.has("autoDowngrade")) {
            autoDowngrade = json.get("autoDowngrade").getAsBoolean();
        }
    }
}
