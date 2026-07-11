package dev.primeclient.core.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.primeclient.core.config.ConfigBinding;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of {@link Theme}s and holder of the active one.
 *
 * <p>{@link #active()} is a plain field read — safe to call every frame.</p>
 */
public final class ThemeManager implements ConfigBinding {

    /** Prime signature look: near-black surfaces, electric blue accent. */
    public static final Theme PRIME_DARK = new Theme(
            "prime-dark", "Prime Dark",
            0xFF3B82F6, // accent
            0xFF6366F1, // accentSecondary
            0xE614161A, // background
            0xF01F2229, // backgroundLight
            0xF2282D36, // surfaceElevated
            0xFFF4F4F5, // foreground
            0xFF9CA3AF, // foregroundMuted
            0x40FFFFFF, // border
            0x99000000, // overlay
            0xFF0F172A, // gradientTop
            0xFF1E293B, // gradientBottom
            0xFF22C55E, // success
            0xFFF59E0B, // warning
            0xFFEF4444  // error
    );

    public static final Theme PRIME_LIGHT = new Theme(
            "prime-light", "Prime Light",
            0xFF2563EB,
            0xFF4F46E5,
            0xE6FAFAFA,
            0xF0FFFFFF,
            0xFFF4F4F5,
            0xFF18181B,
            0xFF52525B,
            0x30000000,
            0x66000000,
            0xFFE2E8F0,
            0xFFF8FAFC,
            0xFF16A34A,
            0xFFD97706,
            0xFFDC2626
    );

    private final Map<String, Theme> themes = new LinkedHashMap<>();
    private Theme active;

    public ThemeManager() {
        register(PRIME_DARK);
        register(PRIME_LIGHT);
        this.active = PRIME_DARK;
    }

    public void register(Theme theme) {
        Theme previous = themes.putIfAbsent(theme.id(), theme);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate theme id: " + theme.id());
        }
    }

    /** The active theme. Never {@code null}. */
    public Theme active() {
        return active;
    }

    /** @throws IllegalArgumentException if no theme has this id */
    public void setActive(String id) {
        Theme theme = themes.get(id);
        if (theme == null) {
            throw new IllegalArgumentException("Unknown theme: " + id);
        }
        this.active = theme;
    }

    public Collection<Theme> all() {
        return Collections.unmodifiableCollection(themes.values());
    }

    @Override
    public String configKey() {
        return "theme";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("active", active.id());
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        JsonElement stored = element.getAsJsonObject().get("active");
        if (stored != null && themes.containsKey(stored.getAsString())) {
            this.active = themes.get(stored.getAsString());
        }
    }
}
