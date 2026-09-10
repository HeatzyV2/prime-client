package dev.primeclient.core.modules.qol;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;

/** Respawns immediately on death. */
public final class AutoRespawnModule extends Module {

    private final MinecraftAdapter adapter;

    public AutoRespawnModule(MinecraftAdapter adapter) {
        super("auto-respawn", "Auto Respawn", "Respawn automatically when you die", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> onTick());
    }

    private void onTick() {
        if (adapter.isDead()) {
            adapter.respawn();
        }
    }
}
