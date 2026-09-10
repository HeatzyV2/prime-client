package dev.primeclient.core.modules.prime;

import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.elements.VoiceSpectrumElement;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Audio spectrum HUD module for voice chat activity. */
public final class VoiceSpectrumModule extends Module {

    private final VoiceSpectrumElement element;

    public VoiceSpectrumModule(HudManager hud, ThemeManager themes) {
        super("voice-spectrum", "Voice Spectrum", "Reactive audio spectrum HUD for voice chat", ModuleCategory.PRIME);
        this.element = hud.register(new VoiceSpectrumElement(themes));
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
