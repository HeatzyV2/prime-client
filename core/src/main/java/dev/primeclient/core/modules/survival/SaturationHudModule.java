package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Hunger saturation for sprint and regen planning. */
public final class SaturationHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public SaturationHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("saturation-hud", "Saturation HUD", "Food saturation level", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "saturation-hud", "Saturation HUD", themes, HudAnchor.TOP_LEFT, 4, 52));
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
        element.setText(String.format("Sat: %.1f", adapter.playerSaturation()));
    }
}
