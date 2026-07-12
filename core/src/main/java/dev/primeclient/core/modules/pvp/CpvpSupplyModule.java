package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Combined crystal PvP hotbar supply — obsidian + crystals + totems. */
public final class CpvpSupplyModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public CpvpSupplyModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("cpvp-supply", "CPvP Supply", "Obsidian, crystals and totems at a glance", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "cpvp-supply", "CPvP Supply", themes, HudAnchor.TOP_LEFT, 4, 68));
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
        int obs = adapter.countHotbarItemsMatching("obsidian");
        int cry = adapter.countHotbarItemsMatching("crystal");
        int tot = adapter.countHotbarItemsMatching("totem");
        element.setText("CPvP O" + obs + " C" + cry + " T" + tot);
    }
}
