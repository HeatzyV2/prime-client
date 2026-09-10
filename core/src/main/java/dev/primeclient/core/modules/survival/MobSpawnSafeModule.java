package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Green/red indicator for mob spawn safety at feet. */
public final class MobSpawnSafeModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public MobSpawnSafeModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("mob-spawn-safe", "Mob Spawn Safe", "Shows if mobs can spawn here", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "mob-spawn-safe", "Mob Spawn Safe", themes, HudAnchor.TOP_LEFT, 4, 60));
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
        boolean safe = adapter.mobSpawnSafeAtFeet();
        int light = adapter.blockLightLevel();
        element.setText(safe ? "Spawn: SAFE (L" + light + ")" : "Spawn: UNSAFE (L" + light + ")");
    }
}
