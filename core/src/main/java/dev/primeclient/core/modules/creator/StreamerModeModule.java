package dev.primeclient.core.modules.creator;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;

/**
 * Hides sensitive HUD elements (coordinates, watermark, server info) for
 * streaming.
 */
public final class StreamerModeModule extends Module {

    private final BooleanSetting hideServer =
            addSetting(new BooleanSetting("hide-server", "Hide server", "Hide server address from HUD", true));

    private final MinecraftAdapter adapter;
    private final HudManager hud;

    private boolean watermarkVisible = true;
    private boolean coordinatesVisible = true;
    private boolean hudWasHidden;

    public StreamerModeModule(HudManager hud, MinecraftAdapter adapter) {
        super("streamer-mode", "Streamer Mode", "Hides coordinates and branding for streams", ModuleCategory.CREATOR);
        this.hud = hud;
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        HudElement watermark = hud.get("watermark");
        if (watermark != null) {
            watermarkVisible = watermark.isVisible();
            watermark.setVisible(false);
        }
        HudElement coordinates = hud.get("coordinates");
        if (coordinates != null) {
            coordinatesVisible = coordinates.isVisible();
            coordinates.setVisible(false);
        }
        hudWasHidden = adapter.isHudHidden();
        adapter.setHudHidden(true);
    }

    @Override
    protected void onDisable() {
        HudElement watermark = hud.get("watermark");
        if (watermark != null) {
            watermark.setVisible(watermarkVisible);
        }
        HudElement coordinates = hud.get("coordinates");
        if (coordinates != null) {
            coordinates.setVisible(coordinatesVisible);
        }
        adapter.setHudHidden(hudWasHidden);
    }

    public boolean hideServer() {
        return hideServer.get();
    }
}
