package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Fall distance and smash tier for mace PvP. */
public final class MaceSmashModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public MaceSmashModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("mace-smash", "Mace Smash", "Fall distance and smash damage tier", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "mace-smash", "Mace Smash", themes, HudAnchor.BOTTOM_CENTER, 0, -36));
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
        float fall = adapter.playerFallDistance();
        element.setText("Smash: " + String.format("%.1f", fall) + " " + PvpFormat.maceTier(fall));
    }
}
