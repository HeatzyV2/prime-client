package dev.primeclient.core.modules.qol;

import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.DoubleSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.state.LowFireState;

/** Lowers the on-fire screen overlay without changing burn damage or duration. */
public final class LowFireModule extends Module {

    private static final float MAX_TRANSLATE = 0.6F;

    private final DoubleSetting heightOffset =
            addSetting(new DoubleSetting(
                    "height",
                    "Height offset",
                    "How far to lower the fire overlay",
                    0.35,
                    0.0,
                    1.0));

    public LowFireModule() {
        super("low-fire", "Low Fire", "Lower the fire overlay while burning", ModuleCategory.QOL);
        listen(ClientTickEvent.class, event -> syncOffset());
    }

    @Override
    protected void onEnable() {
        LowFireState.setActive(true);
        syncOffset();
    }

    @Override
    protected void onDisable() {
        LowFireState.setActive(false);
    }

    private void syncOffset() {
        LowFireState.setHeightOffset((float) (heightOffset.get() * MAX_TRANSLATE));
    }
}
