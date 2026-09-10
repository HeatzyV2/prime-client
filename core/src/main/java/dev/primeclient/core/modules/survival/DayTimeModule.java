package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** In-game clock for planning travel, farming and sleeping. */
public final class DayTimeModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public DayTimeModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("day-time", "Day Time", "Minecraft time and day/night phase", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "day-time", "Day Time", themes, HudAnchor.TOP_LEFT, 4, 4));
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
        long time = adapter.worldDayTime();
        String phase = SurvivalFormat.isNight(time) ? "Night" : "Day";
        element.setText(phase + " " + SurvivalFormat.clock(time));
    }
}
