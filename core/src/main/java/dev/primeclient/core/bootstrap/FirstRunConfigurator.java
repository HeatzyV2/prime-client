package dev.primeclient.core.bootstrap;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.gui.FavoritesManager;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Curated starter modules and favorites for first launch / profile presets. */
public final class FirstRunConfigurator {

    private static final List<String> CORE = List.of(
            "fps", "coordinates", "keystrokes", "cps", "ping",
            "discord-rpc", "crosshair-editor", "prime-cosmetics"
    );

    private static final List<String> PVP_EXTRA = List.of(
            "target-hud", "combo-counter", "hit-color", "armor-hud", "potion-hud"
    );

    private static final List<String> SURVIVAL_EXTRA = List.of(
            "waypoints", "auto-respawn", "item-counter", "toggle-sprint", "death-waypoint"
    );

    private FirstRunConfigurator() {
    }

    public static void applyStarter(PrimeClient client) {
        applyPreset(client.modules(), client.favorites(), "default");
        client.notifications().success("Prime Client",
                "HUD premium activé — Right Shift pour le menu");
    }

    public static void applyPreset(ModuleManager modules, FavoritesManager favorites, String preset) {
        Set<String> ids = new LinkedHashSet<>(CORE);
        if ("pvp".equals(preset)) {
            ids.addAll(PVP_EXTRA);
        } else if ("survival".equals(preset)) {
            ids.addAll(SURVIVAL_EXTRA);
        }
        for (String id : ids) {
            enable(modules, id);
        }
        seedFavorites(favorites, preset);
    }

    private static void enable(ModuleManager modules, String id) {
        Module module = modules.get(id);
        if (module != null && !module.isEnabled()) {
            module.setEnabled(true);
        }
    }

    private static void seedFavorites(FavoritesManager favorites, String preset) {
        favorites.clear();
        favorites.add("crosshair-editor");
        favorites.add("discord-rpc");
        favorites.add("fps");
        favorites.add("keystrokes");
        if ("pvp".equals(preset)) {
            favorites.add("target-hud");
            favorites.add("combo-counter");
        } else if ("survival".equals(preset)) {
            favorites.add("waypoints");
            favorites.add("zoom");
        } else {
            favorites.add("zoom");
            favorites.add("prime-cosmetics");
        }
    }
}
