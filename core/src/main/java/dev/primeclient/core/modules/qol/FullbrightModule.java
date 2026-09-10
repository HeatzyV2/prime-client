package dev.primeclient.core.modules.qol;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.state.FullbrightState;

/** Maximum world brightness without flicker or gamma-slider hacks. */
public final class FullbrightModule extends Module {

    private final MinecraftAdapter adapter;
    private float savedGamma = Float.NaN;

    public FullbrightModule(MinecraftAdapter adapter) {
        super("fullbright", "Fullbright", "See clearly in caves and at night", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> maintainBrightness());
    }

    @Override
    protected void onEnable() {
        savedGamma = Float.NaN;
        FullbrightState.setActive(true);
    }

    @Override
    protected void onDisable() {
        FullbrightState.setActive(false);
        restoreGamma();
    }

    private void maintainBrightness() {
        if (Float.isNaN(savedGamma)) {
            savedGamma = adapter.gamma();
        }
        if (adapter.gamma() < 0.999F) {
            adapter.setGamma(1.0F);
        }
    }

    private void restoreGamma() {
        if (!Float.isNaN(savedGamma)) {
            adapter.setGamma(savedGamma);
            savedGamma = Float.NaN;
        }
    }
}
