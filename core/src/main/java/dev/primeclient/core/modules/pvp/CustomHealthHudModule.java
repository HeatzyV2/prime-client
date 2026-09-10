package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.elements.CustomHealthHudElement;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Custom glass health bar module with smooth damage interpolation. */
public final class CustomHealthHudModule extends Module {

    private final CustomHealthHudElement element;

    public CustomHealthHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("custom-health", "Custom Health Bar", "Smooth glass health bar HUD overlay", ModuleCategory.PVP);
        this.element = hud.register(new CustomHealthHudElement(adapter, themes));
        element.setVisible(false);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }
}
