package dev.primeclient.core.modules.qol;

import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;

/** Custom glass backdrop and Social Hub quick action button for in-game chat. */
public final class GlassChatModule extends Module {

    private final BooleanSetting glassBackdrop = addSetting(new BooleanSetting(
            "glass-backdrop", "Glass Backdrop", "Translucent frosted backdrop for chat lines", true));
    private final BooleanSetting socialShortcut = addSetting(new BooleanSetting(
            "social-shortcut", "Social Hub Shortcut", "Quick Social Hub button inside chat box", true));

    public GlassChatModule() {
        super("glass-chat", "Glass Chat", "Frosted glass style and Social Hub shortcut in chat", ModuleCategory.QOL);
    }

    public boolean glassBackdrop() {
        return isEnabled() && glassBackdrop.get();
    }

    public boolean socialShortcut() {
        return isEnabled() && socialShortcut.get();
    }
}
