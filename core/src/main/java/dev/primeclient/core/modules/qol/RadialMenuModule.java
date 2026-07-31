package dev.primeclient.core.modules.qol;

import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;

/** Quick radial wheel menu module for in-game shortcuts. */
public final class RadialMenuModule extends Module {

    public RadialMenuModule() {
        super("radial-menu", "Radial Menu", "In-game quick radial wheel for profiles and shortcuts", ModuleCategory.QOL);
    }
}
