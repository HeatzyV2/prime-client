package dev.primeclient.core.modules.qol;

import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.ColorSetting;
import dev.primeclient.core.module.DoubleSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.state.HandShaderState;

/** Simple first-person hand / held-item tint. */
public final class HandShaderModule extends Module {

    private final ColorSetting tint = addSetting(new ColorSetting(
            "tint", "Tint", "Hand overlay color", 0xFFFF8A9A));
    private final DoubleSetting intensity = addSetting(new DoubleSetting(
            "intensity", "Intensity", "How strongly the tint is applied", 0.35, 0.0, 1.0));

    public HandShaderModule() {
        super("hand-shader", "Hand Shader", "Tints your first-person hand and held item",
                ModuleCategory.QOL);
        listen(ClientTickEvent.class, event -> sync());
    }

    @Override
    protected void onEnable() {
        HandShaderState.setActive(true);
        sync();
    }

    @Override
    protected void onDisable() {
        HandShaderState.reset();
    }

    private void sync() {
        HandShaderState.setArgb(tint.get());
        HandShaderState.setIntensity((float) intensity.get());
    }
}
