package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.IntSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Hunger level — sprint and regen depend on it in long fights. */
public final class FoodLevelModule extends Module {

    private final IntSetting warnBelow = addSetting(new IntSetting(
            "warn-below", "Warn below", "Highlight when food is at or below this", 8, 1, 19));

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public FoodLevelModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("food-level", "Food Level", "Hunger for sprint and regeneration", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "food-level", "Food Level", themes, HudAnchor.TOP_LEFT, 4, 20));
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
        int food = adapter.playerFoodLevel();
        String tag = food <= warnBelow.get() ? " LOW" : "";
        element.setText("Food: " + food + "/20" + tag);
    }
}
