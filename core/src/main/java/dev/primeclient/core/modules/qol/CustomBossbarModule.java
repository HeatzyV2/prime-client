package dev.primeclient.core.modules.qol;

import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.elements.CustomBossbarElement;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Custom glass bossbar overlay module. */
public final class CustomBossbarModule extends Module {

    private final CustomBossbarElement element;

    public CustomBossbarModule(HudManager hud, ThemeManager themes) {
        super("custom-bossbar", "Custom Bossbar", "Frosted glass pill overhaul for bossbars", ModuleCategory.QOL);
        this.element = hud.register(new CustomBossbarElement(themes));
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
