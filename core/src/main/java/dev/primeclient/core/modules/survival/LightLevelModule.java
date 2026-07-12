package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Block light level — mob spawning and crop growth. */
public final class LightLevelModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public LightLevelModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("light-level", "Light Level", "Block light at your position", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "light-level", "Light Level", themes, HudAnchor.TOP_LEFT, 4, 36));
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
        int light = adapter.blockLightLevel();
        String tag = light <= 7 ? " (mobs)" : "";
        element.setText("Light: " + light + tag);
    }
}
