package dev.primeclient.core.modules.qol;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.keybind.Keybind;
import dev.primeclient.core.keybind.KeybindManager;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.profile.ProfileManager;
import dev.primeclient.core.state.RadialMenuState;

import java.util.Objects;

/**
 * In-game radial wheel for quick profile switches and shortcuts.
 *
 * <p>Default hotkey: {@code G}. Opens while the module is enabled and no screen is up.
 * PVP / Survival slices call {@link ProfileManager#switchTo(String)} via {@code RadialMenuRenderer}.</p>
 */
public final class RadialMenuModule extends Module {

    /** GLFW_KEY_G */
    private static final int DEFAULT_KEY = 71;

    private final MinecraftAdapter adapter;

    public RadialMenuModule(ProfileManager profiles, MinecraftAdapter adapter, KeybindManager keybinds) {
        super("radial-menu", "Radial Menu",
                "In-game quick radial wheel for profiles and shortcuts", ModuleCategory.QOL);
        Objects.requireNonNull(profiles, "profiles");
        this.adapter = adapter;
        keybinds.register(new Keybind("radial-menu", "Radial Menu", "QoL", DEFAULT_KEY)
                .onPress(this::toggleWheel));
    }

    @Override
    protected void onDisable() {
        RadialMenuState.setOpen(false);
    }

    private void toggleWheel() {
        if (!isEnabled()) {
            return;
        }
        if (adapter.isScreenOpen()) {
            return;
        }
        RadialMenuState.setOpen(!RadialMenuState.open());
    }
}
