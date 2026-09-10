package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.elements.CustomPotionGridElement;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Potion grid status HUD module with expiry alerts. */
public final class CustomPotionGridModule extends Module {

    private final CustomPotionGridElement element;

    public CustomPotionGridModule(HudManager hud, ThemeManager themes) {
        super("custom-potion-grid", "Custom Potion Grid", "Glass potion status card with expiry pulse", ModuleCategory.PVP);
        this.element = hud.register(new CustomPotionGridElement(themes));
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
