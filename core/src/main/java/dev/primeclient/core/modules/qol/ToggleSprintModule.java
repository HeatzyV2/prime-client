package dev.primeclient.core.modules.qol;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;

/** Keeps sprinting while moving forward. */
public final class ToggleSprintModule extends Module {

    private final MinecraftAdapter adapter;

    public ToggleSprintModule(MinecraftAdapter adapter) {
        super("toggle-sprint", "Toggle Sprint", "Sprint automatically while moving forward", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> onTick());
    }

    private void onTick() {
        if (adapter.isMovingForward()) {
            adapter.setSprinting(true);
        }
    }
}
