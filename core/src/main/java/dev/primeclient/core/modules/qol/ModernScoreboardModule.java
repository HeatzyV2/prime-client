package dev.primeclient.core.modules.qol;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.elements.ModernScoreboardElement;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Modern glass scoreboard HUD module. */
public final class ModernScoreboardModule extends Module {

    private final ModernScoreboardElement element;

    public ModernScoreboardModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("modern-scoreboard", "Modern Scoreboard", "Glass-styled scoreboard sidebar replacement", ModuleCategory.QOL);
        this.element = hud.register(new ModernScoreboardElement(adapter, themes));
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
