package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Growth stage of the crop block under the player. */
public final class CropGrowthHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public CropGrowthHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("crop-growth-hud", "Crop Growth HUD", "Shows crop growth under you", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "crop-growth-hud", "Crop Growth", themes, HudAnchor.TOP_LEFT, 4, 48));
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
        int stage = adapter.cropGrowthStage();
        if (stage < 0) {
            String block = adapter.blockUnderPlayerName();
            element.setText(block.isEmpty() ? "Crop: —" : "Block: " + block);
            return;
        }
        element.setText("Crop: " + stage + "/7");
    }
}
