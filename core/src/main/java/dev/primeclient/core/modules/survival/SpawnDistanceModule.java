package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Distance to world spawn. */
public final class SpawnDistanceModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public SpawnDistanceModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("spawn-distance", "Spawn Distance", "Blocks from world spawn", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "spawn-distance", "Spawn Distance", themes, HudAnchor.TOP_RIGHT, -4, 52));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> refresh());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void refresh() {
        element.setText("Spawn: " + (int) adapter.spawnDistance() + "m");
    }
}
