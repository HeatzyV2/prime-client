package dev.primeclient.core.modules.qol;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;

/** Keeps sneaking while enabled. */
public final class ToggleSneakModule extends Module {

    private final MinecraftAdapter adapter;

    public ToggleSneakModule(MinecraftAdapter adapter) {
        super("toggle-sneak", "Toggle Sneak", "Stay sneaking while enabled", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> onTick());
    }

    private void onTick() {
        adapter.setSneaking(true);
    }
}
