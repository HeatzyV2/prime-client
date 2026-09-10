package dev.primeclient.core.config;

import com.google.gson.JsonObject;
import dev.primeclient.core.PrimeClient;

/**
 * Migrates profile JSON from older {@code schemaVersion} values up to
 * {@link ConfigManager#SCHEMA_VERSION}.
 *
 * <p>Each step is idempotent and mutates {@code root} in place. Unknown future
 * versions are left untouched (best-effort load with a warning elsewhere).</p>
 */
public final class ConfigMigrator {

    private ConfigMigrator() {
    }

    /**
     * @return the schema version after migration (may equal {@code fromVersion}
     *         when already current or newer than supported)
     */
    public static int migrate(JsonObject root, int fromVersion) {
        int version = fromVersion;
        if (version < 1) {
            version = 1;
        }
        while (version < ConfigManager.SCHEMA_VERSION) {
            int next = version + 1;
            migrateStep(root, version, next);
            version = next;
        }
        root.addProperty("schemaVersion", version);
        return version;
    }

    private static void migrateStep(JsonObject root, int from, int to) {
        PrimeClient.LOGGER.info("Migrating config schema {} → {}", from, to);
        switch (to) {
            case 2 -> migrateTo2(root);
            case 3 -> migrateTo3(root);
            case 4 -> migrateTo4(root);
            case 5 -> migrateTo5(root);
            default -> {
            }
        }
    }

    /** Legacy profiles without schemaVersion were treated as v2. */
    private static void migrateTo2(JsonObject root) {
        // No structural change — stamp only.
    }

    /**
     * v3 introduced HUD {@code _order}, lock, tint, opacity fields.
     * Older element sections remain valid; nothing to rewrite.
     */
    private static void migrateTo3(JsonObject root) {
        // Best-effort: ensure hud section is an object if present.
        if (root.has("hud") && !root.get("hud").isJsonObject()) {
            root.remove("hud");
        }
    }

    /**
     * v4: HUD {@code visible} is layout-only; module enable is separate ({@code active}).
     * Existing {@code visible:false} stays as user hide preference — no rewrite needed.
     * Also normalizes missing clickgui search leftovers.
     */
    private static void migrateTo4(JsonObject root) {
        if (root.has("clickgui") && root.get("clickgui").isJsonObject()) {
            JsonObject clickgui = root.getAsJsonObject("clickgui");
            // Drop obsolete panel-position noise if empty object keys linger — keep as-is.
            clickgui.remove("searchPanel");
        }
    }

    /**
     * v5: strip retired Performance modules superseded by Performance Profiles
     * (and remove forced-GC / anti-performance modules from product config).
     */
    private static void migrateTo5(JsonObject root) {
        if (root.has("modules") && root.get("modules").isJsonObject()) {
            JsonObject modules = root.getAsJsonObject("modules");
            for (String id : REMOVED_PERFORMANCE_MODULE_IDS) {
                modules.remove(id);
            }
        }
        if (root.has("keybinds") && root.get("keybinds").isJsonObject()) {
            JsonObject keybinds = root.getAsJsonObject("keybinds");
            for (String id : REMOVED_PERFORMANCE_MODULE_IDS) {
                keybinds.remove("module." + id);
            }
        }
    }

    private static final String[] REMOVED_PERFORMANCE_MODULE_IDS = {
            "fps-booster",
            "entity-culling",
            "particle-optimizer",
            "ram-cleaner",
            "chunk-optimizer",
            "animation-optimizer",
            "fast-loading"
    };
}
