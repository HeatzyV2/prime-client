package dev.primeclient.core.gui;

import com.google.gson.JsonArray;
import dev.primeclient.core.event.EventBus;
import dev.primeclient.core.keybind.KeybindManager;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.module.ModuleManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentlyUsedManagerTest {

    @Test
    void touchMovesToFrontAndCaps() {
        RecentlyUsedManager recent = new RecentlyUsedManager();
        for (int i = 0; i < RecentlyUsedManager.MAX + 3; i++) {
            recent.touch("m" + i);
        }
        JsonArray json = recent.toJson();
        assertEquals(RecentlyUsedManager.MAX, json.size());
        assertEquals("m" + (RecentlyUsedManager.MAX + 2), json.get(0).getAsString());
    }

    @Test
    void roundTripPreservesOrder() {
        RecentlyUsedManager recent = new RecentlyUsedManager();
        recent.touch("a");
        recent.touch("b");
        recent.touch("a");
        JsonArray saved = recent.toJson();
        assertEquals("a", saved.get(0).getAsString());
        assertEquals("b", saved.get(1).getAsString());

        RecentlyUsedManager loaded = new RecentlyUsedManager();
        loaded.fromJson(saved);
        assertEquals(saved, loaded.toJson());
    }

    @Test
    void resolveSkipsMissingModules() {
        ModuleManager modules = new ModuleManager(new EventBus(), new KeybindManager());
        modules.register(new Module("zoom", "Zoom", "", ModuleCategory.QOL) {});
        RecentlyUsedManager recent = new RecentlyUsedManager();
        recent.touch("missing");
        recent.touch("zoom");
        assertEquals(1, recent.resolve(modules).size());
        assertEquals("zoom", recent.resolve(modules).get(0).id());
    }
}
