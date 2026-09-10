package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Y level with mining depth hints. */
public final class DepthHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public DepthHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("depth-hud", "Depth HUD", "Altitude and mining layer hints", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "depth-hud", "Depth HUD", themes, HudAnchor.BOTTOM_LEFT, 4, -16));
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
        int y = (int) Math.floor(adapter.playerY());
        element.setText("Y: " + y + SurvivalFormat.depthHint(y));
    }
}
