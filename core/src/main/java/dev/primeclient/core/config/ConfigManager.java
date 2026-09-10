package dev.primeclient.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.primeclient.core.PrimeClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists all registered {@link ConfigBinding}s into a single JSON file.
 *
 * <p>One config file = one profile (see
 * {@link dev.primeclient.core.profile.ProfileManager}). Writes are atomic:
 * the file is written to a temp sibling then moved, so a crash mid-save never
 * corrupts an existing config.</p>
 *
 * <p>Not thread-safe by design: all access happens on the client thread.</p>
 */
public final class ConfigManager {

    /** Bumped when the root profile JSON shape changes in a breaking way. */
    public static final int SCHEMA_VERSION = 5;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, ConfigBinding> bindings = new LinkedHashMap<>();

    /**
     * Registers a binding. Fails fast on duplicate keys — two subsystems
     * silently sharing a key would corrupt each other's state.
     */
    public void register(ConfigBinding binding) {
        ConfigBinding previous = bindings.putIfAbsent(binding.configKey(), binding);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate config key: " + binding.configKey());
        }
    }

    /** Serializes every binding into {@code file}, creating parent directories. */
    public void saveTo(Path file) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        for (ConfigBinding binding : bindings.values()) {
            try {
                root.add(binding.configKey(), binding.saveConfig());
            } catch (RuntimeException e) {
                PrimeClient.LOGGER.error("Failed to serialize config section '{}'", binding.configKey(), e);
            }
        }
        atomicWrite(file, gson.toJson(root));
    }

    /**
     * Loads {@code file} and dispatches each section to its binding.
     *
     * <p>Missing file, unreadable JSON or a broken section never abort the
     * client: affected bindings simply keep their current (default) state.
     * Corrupt files are quarantined to {@code *.corrupt-<timestamp>}.</p>
     */
    public void loadFrom(Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            PrimeClient.LOGGER.error("Failed to read config {} — keeping defaults", file, e);
            quarantineCorrupt(file);
            return;
        }
        int version = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 2;
        if (version > SCHEMA_VERSION) {
            PrimeClient.LOGGER.warn(
                    "Config {} schemaVersion {} is newer than supported {} — loading best-effort",
                    file, version, SCHEMA_VERSION);
        } else if (version < SCHEMA_VERSION) {
            try {
                version = ConfigMigrator.migrate(root, version);
            } catch (RuntimeException e) {
                PrimeClient.LOGGER.error("Config migration failed for {} — loading best-effort", file, e);
            }
        }
        for (ConfigBinding binding : bindings.values()) {
            JsonElement section = root.get(binding.configKey());
            if (section == null) {
                continue;
            }
            try {
                binding.loadConfig(section);
            } catch (RuntimeException e) {
                PrimeClient.LOGGER.error("Failed to load config section '{}' — keeping defaults", binding.configKey(), e);
            }
        }
    }

    /** Exports all bindings into one JSON object (local backup / import). */
    public JsonObject exportAll() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        for (ConfigBinding binding : bindings.values()) {
            try {
                root.add(binding.configKey(), binding.saveConfig());
            } catch (RuntimeException e) {
                PrimeClient.LOGGER.error("Failed to export config section '{}'", binding.configKey(), e);
            }
        }
        return root;
    }

    /** Imports all sections from a full config snapshot. */
    public void importAll(JsonElement rootElement) {
        if (rootElement == null || !rootElement.isJsonObject()) {
            return;
        }
        JsonObject root = rootElement.getAsJsonObject();
        int version = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 2;
        if (version < SCHEMA_VERSION) {
            try {
                ConfigMigrator.migrate(root, version);
            } catch (RuntimeException e) {
                PrimeClient.LOGGER.error("Config import migration failed — loading best-effort", e);
            }
        }
        for (ConfigBinding binding : bindings.values()) {
            JsonElement section = root.get(binding.configKey());
            if (section == null) {
                continue;
            }
            try {
                binding.loadConfig(section);
            } catch (RuntimeException e) {
                PrimeClient.LOGGER.error("Failed to import config section '{}'", binding.configKey(), e);
            }
        }
    }

    /**
     * Reloads a single section from disk (launcher bridge cosmetics sync).
     * Missing section or binding is a no-op.
     */
    public void reloadSection(Path file, String key) {
        if (key == null || key.isBlank() || !Files.isRegularFile(file)) {
            return;
        }
        ConfigBinding binding = bindings.get(key);
        if (binding == null) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonElement section = root.get(key);
            if (section == null) {
                return;
            }
            binding.loadConfig(section);
        } catch (IOException | RuntimeException e) {
            PrimeClient.LOGGER.debug("Failed to reload config section '{}' from {}", key, file);
        }
    }

    public static void atomicWrite(Path file, String contents) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            PrimeClient.LOGGER.error("Failed to save config to {}", file, e);
        }
    }

    private static void quarantineCorrupt(Path file) {
        try {
            Path quarantine = file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis());
            Files.move(file, quarantine, StandardCopyOption.REPLACE_EXISTING);
            PrimeClient.LOGGER.warn("Quarantined corrupt config to {}", quarantine);
        } catch (IOException moveFailed) {
            PrimeClient.LOGGER.error("Failed to quarantine corrupt config {}", file, moveFailed);
        }
    }
}
