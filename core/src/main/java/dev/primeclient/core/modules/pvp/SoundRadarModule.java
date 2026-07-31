package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.elements.SoundRadarElement;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Directional sound radar module for footsteps and combat sounds. */
public final class SoundRadarModule extends Module {

    private final SoundRadarElement element;

    public SoundRadarModule(HudManager hud, ThemeManager themes) {
        super("sound-radar", "Sound Radar", "Directional HUD radar for nearby footsteps and sounds", ModuleCategory.PVP);
        this.element = hud.register(new SoundRadarElement(themes));
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
