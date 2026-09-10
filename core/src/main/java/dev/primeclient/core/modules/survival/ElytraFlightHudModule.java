package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Elytra flight status, fireworks, and glide hints. */
public final class ElytraFlightHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ElytraFlightHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("elytra-flight-hud", "Elytra Flight HUD", "Flying status and firework count", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "elytra-flight-hud", "Elytra Flight", themes, HudAnchor.BOTTOM_LEFT, 4, -32));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> refresh());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void refresh() {
        int rockets = adapter.fireworkRocketCount();
        if (adapter.playerFallFlying()) {
            element.setText("Elytra: GLIDING · " + rockets + " rockets");
            return;
        }
        String held = adapter.heldItemName();
        if (held.toLowerCase().contains("elytra")) {
            element.setText("Elytra: ready · " + rockets + " rockets · jump+fall");
            return;
        }
        element.setText("Elytra: idle · " + rockets + " rockets");
    }
}
