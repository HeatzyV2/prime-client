package dev.primeclient.core.modules.creator;

import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;

import java.util.HashSet;
import java.util.Set;

/** Hides coords and server IP sensitive HUD elements for streams. */
public final class StreamSafeHudModule extends Module {

    private final BooleanSetting hideCoords =
            addSetting(new BooleanSetting("hide-coords", "Hide coords", "Hide coordinates HUD", true));
    private final BooleanSetting hideServer =
            addSetting(new BooleanSetting("hide-server", "Hide server", "Hide server-related HUD", true));

    private final HudManager hud;
    private final Set<String> hiddenIds = new HashSet<>();
    private final Set<String> savedVisible = new HashSet<>();

    public StreamSafeHudModule(HudManager hud) {
        super("stream-safe-hud", "Stream Safe HUD", "Hide sensitive HUD for streams", ModuleCategory.CREATOR);
        this.hud = hud;
        listen(ClientTickEvent.class, event -> apply());
    }

    @Override
    protected void onEnable() {
        apply();
    }

    @Override
    protected void onDisable() {
        restore();
    }

    private void apply() {
        if (hideCoords.get()) {
            hideElement("coordinates");
            hideElement("chunk-coords");
            hideElement("biome-coords");
        }
        if (hideServer.get()) {
            hideElement("ping");
            hideElement("server-switcher");
            hideElement("prime-account");
        }
    }

    private void hideElement(String id) {
        if (hiddenIds.contains(id)) {
            return;
        }
        HudElement element = hud.get(id);
        if (element != null && element.isVisible()) {
            savedVisible.add(id);
            element.setVisible(false);
        }
        hiddenIds.add(id);
    }

    private void restore() {
        for (String id : savedVisible) {
            HudElement element = hud.get(id);
            if (element != null) {
                element.setVisible(true);
            }
        }
        savedVisible.clear();
        hiddenIds.clear();
    }
}
