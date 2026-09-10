package dev.primeclient.core.modules.qol;

import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.state.NoRainState;

/** Hides rain and snow particles on the client without changing server weather. */
public final class NoRainModule extends Module {

    public NoRainModule() {
        super("no-rain", "No Rain", "Hide rain and snow on your screen", ModuleCategory.QOL);
    }

    @Override
    protected void onEnable() {
        NoRainState.setActive(true);
    }

    @Override
    protected void onDisable() {
        NoRainState.setActive(false);
    }
}
