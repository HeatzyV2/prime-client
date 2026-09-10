package dev.primeclient.core.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.primeclient.core.config.ConfigBinding;
import dev.primeclient.core.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileManagerTest {

    private static final class StringBinding implements ConfigBinding {
        private final String key;
        String value;

        StringBinding(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String configKey() {
            return key;
        }

        @Override
        public JsonElement saveConfig() {
            return new JsonPrimitive(value);
        }

        @Override
        public void loadConfig(JsonElement element) {
            this.value = element.getAsString();
        }
    }

    @TempDir
    Path dir;

    private ConfigManager config;
    private StringBinding greeting;
    private ProfileManager profiles;

    @BeforeEach
    void setUp() {
        config = new ConfigManager();
        greeting = new StringBinding("greeting", "hello");
        config.register(greeting);
        profiles = new ProfileManager(config, dir);
        profiles.loadInitial();
        profiles.saveActive();
    }

    @Test
    void createWritesNewProfileFile() {
        profiles.create("arena");
        assertTrue(Files.isRegularFile(dir.resolve("profiles").resolve("arena.json")));
        assertTrue(profiles.listProfiles().contains("arena"));
        assertEquals(ProfileManager.DEFAULT_PROFILE, profiles.activeProfile());
    }

    @Test
    void createRejectsDuplicateName() {
        profiles.create("arena");
        assertThrows(IllegalArgumentException.class, () -> profiles.create("arena"));
    }

    @Test
    void duplicateCopiesSourceToNewName() {
        greeting.value = "from-default";
        profiles.saveActive();
        profiles.duplicate(ProfileManager.DEFAULT_PROFILE, "copy");

        greeting.value = "changed";
        profiles.switchTo("copy");
        assertEquals("from-default", greeting.value);
        assertEquals("copy", profiles.activeProfile());
    }

    @Test
    void deleteRemovesProfileAndGuardsDefault() {
        profiles.create("temp");
        profiles.switchTo("temp");
        greeting.value = "temp-value";
        profiles.saveActive();

        profiles.delete("temp");
        assertFalse(Files.isRegularFile(dir.resolve("profiles").resolve("temp.json")));
        assertEquals(ProfileManager.DEFAULT_PROFILE, profiles.activeProfile());

        assertThrows(IllegalArgumentException.class,
                () -> profiles.delete(ProfileManager.DEFAULT_PROFILE));
        assertTrue(Files.isRegularFile(dir.resolve("profiles").resolve("default.json")));
    }

    @Test
    void listProfilesIncludesCreatedNames() {
        profiles.create("pvp");
        profiles.create("survival");
        List<String> names = profiles.listProfiles();
        assertTrue(names.contains("default"));
        assertTrue(names.contains("pvp"));
        assertTrue(names.contains("survival"));
    }
}
