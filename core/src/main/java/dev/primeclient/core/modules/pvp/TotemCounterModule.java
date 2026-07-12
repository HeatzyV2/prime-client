package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Totem of undying count in inventory. */
public final class TotemCounterModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public TotemCounterModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("totem-counter", "Totem Counter", "How many totems you are carrying", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "totem-counter", "Totem Counter", themes, HudAnchor.TOP_LEFT, 4, 4));
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
        int count = adapter.countItemsMatching("totem");
        element.setText("Totems: " + count);
    }
}
