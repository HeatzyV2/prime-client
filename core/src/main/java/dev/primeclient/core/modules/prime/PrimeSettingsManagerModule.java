package dev.primeclient.core.modules.prime;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.module.ModuleManager;
import dev.primeclient.core.module.Setting;

/** Opens the ClickGUI and optionally resets all module settings. */
public final class PrimeSettingsManagerModule extends Module {

    private final BooleanSetting resetOnDisable =
            addSetting(new BooleanSetting("reset-on-disable", "Reset on disable", "Reset all modules when turned off", false));

    private final ModuleManager modules;
    private final MinecraftAdapter adapter;

    public PrimeSettingsManagerModule(ModuleManager modules, MinecraftAdapter adapter) {
        super("prime-settings-manager", "Prime Settings Manager", "Opens the ClickGUI for module settings", ModuleCategory.PRIME);
        this.modules = modules;
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        adapter.openClickGui();
        setEnabled(false);
    }

    @Override
    protected void onDisable() {
        if (!resetOnDisable.get()) {
            return;
        }
        for (Module module : modules.all()) {
            for (Setting setting : module.settings()) {
                setting.reset();
            }
            module.setEnabled(false);
        }
    }
}
