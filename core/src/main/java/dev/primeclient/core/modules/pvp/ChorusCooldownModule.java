package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Chorus fruit cooldown for pearl-chorus escapes. */
public final class ChorusCooldownModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ChorusCooldownModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("chorus-cooldown", "Chorus Cooldown", "Chorus fruit teleport readiness", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "chorus-cooldown", "Chorus Cooldown", themes, HudAnchor.BOTTOM_LEFT, 4, -96));
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
        element.setText(PvpFormat.cooldown("Chorus", adapter.itemCooldownReady("chorus")));
    }
}
