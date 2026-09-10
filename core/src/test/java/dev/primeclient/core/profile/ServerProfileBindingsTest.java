package dev.primeclient.core.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServerProfileBindingsTest {

    @Test
    void bindsAndNormalizesServerAddress() {
        ServerProfileBindings bindings = new ServerProfileBindings();
        bindings.bind("Hypixel.NET:25565", "pvp");
        assertEquals("pvp", bindings.profileForServer("hypixel.net:25565"));
        assertEquals("pvp", bindings.profileForServer("HYPIXEL.NET:25565"));
    }

    @Test
    void singleplayerKeyIsStable() {
        ServerProfileBindings bindings = new ServerProfileBindings();
        bindings.bind("Singleplayer", "survival");
        assertEquals("survival", bindings.profileForServer(null));
        assertEquals("survival", bindings.profileForServer(""));
    }

    @Test
    void roundTripsJson() {
        ServerProfileBindings bindings = new ServerProfileBindings();
        bindings.bind("play.example.com", "default");
        var json = bindings.saveConfig();
        ServerProfileBindings loaded = new ServerProfileBindings();
        loaded.loadConfig(json);
        assertEquals("default", loaded.profileForServer("play.example.com"));
        loaded.unbind("play.example.com");
        assertNull(loaded.profileForServer("play.example.com"));
    }
}
