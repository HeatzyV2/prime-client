package dev.primeclient.core.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

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

    @Test
    void saveThenLoadRoundTrips(@TempDir Path dir) {
        ConfigManager manager = new ConfigManager();
        StringBinding binding = new StringBinding("greeting", "hello");
        manager.register(binding);

        Path file = dir.resolve("profiles").resolve("default.json");
        manager.saveTo(file);
        assertTrue(Files.isRegularFile(file));

        binding.value = "changed";
        manager.loadFrom(file);
        assertEquals("hello", binding.value);
    }

    @Test
    void loadMissingFileKeepsDefaults(@TempDir Path dir) {
        ConfigManager manager = new ConfigManager();
        StringBinding binding = new StringBinding("greeting", "default");
        manager.register(binding);

        manager.loadFrom(dir.resolve("nope.json"));
        assertEquals("default", binding.value);
    }

    @Test
    void corruptFileKeepsDefaults(@TempDir Path dir) throws Exception {
        ConfigManager manager = new ConfigManager();
        StringBinding binding = new StringBinding("greeting", "default");
        manager.register(binding);

        Path file = dir.resolve("broken.json");
        Files.writeString(file, "{ not json !!!");
        manager.loadFrom(file);
        assertEquals("default", binding.value);
        assertTrue(Files.list(dir)
                .anyMatch(p -> p.getFileName().toString().startsWith("broken.json.corrupt-")));
    }

    @Test
    void saveWritesSchemaVersion(@TempDir Path dir) throws Exception {
        ConfigManager manager = new ConfigManager();
        manager.register(new StringBinding("greeting", "hello"));
        Path file = dir.resolve("profiles").resolve("default.json");
        manager.saveTo(file);
        String raw = Files.readString(file);
        assertTrue(raw.contains("\"schemaVersion\": " + ConfigManager.SCHEMA_VERSION));
    }

    @Test
    void brokenSectionDoesNotAffectOthers(@TempDir Path dir) throws Exception {
        ConfigManager manager = new ConfigManager();
        StringBinding first = new StringBinding("first", "a");
        StringBinding second = new StringBinding("second", "b");
        manager.register(first);
        manager.register(second);

        Path file = dir.resolve("partial.json");
        Files.writeString(file, "{\"first\": {\"unexpected\": \"object\"}, \"second\": \"loaded\"}");
        manager.loadFrom(file);

        assertEquals("a", first.value);
        assertEquals("loaded", second.value);
    }

    @Test
    void duplicateKeyIsRejected() {
        ConfigManager manager = new ConfigManager();
        manager.register(new StringBinding("dup", "x"));
        assertThrows(IllegalArgumentException.class, () -> manager.register(new StringBinding("dup", "y")));
    }

    @Test
    void migratesLegacySchemaToCurrent(@TempDir Path dir) throws Exception {
        ConfigManager manager = new ConfigManager();
        StringBinding binding = new StringBinding("greeting", "default");
        manager.register(binding);

        Path file = dir.resolve("legacy.json");
        Files.writeString(file, "{\"schemaVersion\":2,\"greeting\":\"hello\",\"clickgui\":{\"searchPanel\":true}}");
        manager.loadFrom(file);

        assertEquals("hello", binding.value);
        String raw = Files.readString(file);
        // migrate mutates in-memory root only; next save stamps current schema
        manager.saveTo(file);
        raw = Files.readString(file);
        assertTrue(raw.contains("\"schemaVersion\": " + ConfigManager.SCHEMA_VERSION));
    }

    @Test
    void migratorStampsSchemaVersion() {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("schemaVersion", 2);
        com.google.gson.JsonObject clickgui = new com.google.gson.JsonObject();
        clickgui.addProperty("searchPanel", true);
        root.add("clickgui", clickgui);

        int version = ConfigMigrator.migrate(root, 2);
        assertEquals(ConfigManager.SCHEMA_VERSION, version);
        assertEquals(ConfigManager.SCHEMA_VERSION, root.get("schemaVersion").getAsInt());
        assertTrue(!root.getAsJsonObject("clickgui").has("searchPanel"));
    }

    @Test
    void migratorV5StripsRetiredPerformanceModules() {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("schemaVersion", 4);
        com.google.gson.JsonObject modules = new com.google.gson.JsonObject();
        modules.add("ram-cleaner", new com.google.gson.JsonObject());
        modules.add("fps-booster", new com.google.gson.JsonObject());
        modules.add("performance-profiles", new com.google.gson.JsonObject());
        root.add("modules", modules);
        com.google.gson.JsonObject keybinds = new com.google.gson.JsonObject();
        keybinds.addProperty("module.ram-cleaner", 82);
        keybinds.addProperty("module.dynamic-fps", 83);
        root.add("keybinds", keybinds);

        int version = ConfigMigrator.migrate(root, 4);
        assertEquals(5, version);
        assertFalse(root.getAsJsonObject("modules").has("ram-cleaner"));
        assertFalse(root.getAsJsonObject("modules").has("fps-booster"));
        assertTrue(root.getAsJsonObject("modules").has("performance-profiles"));
        assertFalse(root.getAsJsonObject("keybinds").has("module.ram-cleaner"));
        assertTrue(root.getAsJsonObject("keybinds").has("module.dynamic-fps"));
    }
}
