package dev.primeclient.core.modules.survival;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Current weather — useful before travel or mob farms. */
public final class WeatherHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public WeatherHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("weather-hud", "Weather HUD", "Rain and thunder status", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "weather-hud", "Weather HUD", themes, HudAnchor.TOP_LEFT, 4, 20));
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
        if (adapter.worldThundering()) {
            element.setText("Weather: Storm");
        } else if (adapter.worldRaining()) {
            element.setText("Weather: Rain");
        } else {
            element.setText("Weather: Clear");
        }
    }
}
