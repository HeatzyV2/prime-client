package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Shows when you or your target are blocking with a shield. */
public final class ShieldStatusModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ShieldStatusModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("shield-status", "Shield Status", "Your block state and target shield usage", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "shield-status", "Shield Status", themes, HudAnchor.BOTTOM_RIGHT, -4, -48));
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
        String self = adapter.playerBlocking() ? "Blocking" : "Open";
        String target = adapter.hasTarget()
                ? (adapter.targetBlocking() ? "Target: Block" : "Target: Open")
                : "Target: —";
        element.setText("Shield: " + self + " | " + target);
    }
}
