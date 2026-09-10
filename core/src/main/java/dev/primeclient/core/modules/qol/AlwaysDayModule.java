package dev.primeclient.core.modules.qol;

import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.state.AlwaysDayState;

/** Forces a bright daytime sky on the client without changing server time. */
public final class AlwaysDayModule extends Module {

    public AlwaysDayModule() {
        super("always-day", "Always Day", "Render the world as daytime on your screen", ModuleCategory.QOL);
    }

    @Override
    protected void onEnable() {
        AlwaysDayState.setActive(true);
    }

    @Override
    protected void onDisable() {
        AlwaysDayState.setActive(false);
    }
}
