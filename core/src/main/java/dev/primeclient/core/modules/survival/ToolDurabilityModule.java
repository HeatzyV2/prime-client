package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Held tool durability for mining and combat. */
public final class ToolDurabilityModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ToolDurabilityModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("tool-durability", "Tool Durability", "Durability of item in main hand", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "tool-durability", "Tool Durability", themes, HudAnchor.BOTTOM_RIGHT, -4, -116));
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
        int percent = adapter.heldItemDurabilityPercent();
        if (percent < 0) {
            element.setText("Tool: —");
        } else {
            element.setText("Tool: " + percent + "%");
        }
    }
}
