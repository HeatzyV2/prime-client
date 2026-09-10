package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Wind charge cooldown for knockback combos. */
public final class WindChargeCooldownModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public WindChargeCooldownModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("wind-cooldown", "Wind Cooldown", "Wind charge readiness", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "wind-cooldown", "Wind Cooldown", themes, HudAnchor.BOTTOM_LEFT, 4, -80));
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
        element.setText(PvpFormat.cooldown("Wind", adapter.itemCooldownReady("wind")));
    }
}
