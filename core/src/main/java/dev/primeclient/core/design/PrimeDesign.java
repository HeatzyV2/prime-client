package dev.primeclient.core.design;

/**
 * Prime Client V3 design tokens — spacing, radii, motion, elevation, typography
 * and shared component sizes. Colors live on {@link dev.primeclient.core.theme.Theme}.
 *
 * <p>UI code should prefer these constants over magic numbers so ClickGUI,
 * HUD Editor, tooltips and notifications stay visually coherent.</p>
 */
public final class PrimeDesign {

    /** Synced from {@code gradle.properties} {@code mod_version} at build time. */
    public static final String VERSION = PrimeVersion.VERSION;
    public static final String TAGLINE = "Premium Minecraft Client";

    // ------------------------------------------------------------------
    // Spacing (px, GUI scaled)
    // ------------------------------------------------------------------
    public static final int SPACE_XS = 2;
    public static final int SPACE_SM = 4;
    public static final int SPACE_MD = 8;
    public static final int SPACE_LG = 12;
    public static final int SPACE_XL = 16;
    public static final int SPACE_2XL = 24;

    // ------------------------------------------------------------------
    // Radii
    // ------------------------------------------------------------------
    public static final int RADIUS_SM = 2;
    public static final int RADIUS_MD = 4;
    public static final int RADIUS_LG = 8;
    public static final int RADIUS_PILL = 999;

    // ------------------------------------------------------------------
    // Stroke / elevation
    // ------------------------------------------------------------------
    public static final int STROKE_THIN = 1;
    public static final int STROKE_MD = 2;
    /** Soft shadow alpha (ARGB channel hints for UiChrome). */
    public static final int SHADOW_SOFT = 0x70000000;
    public static final int SHADOW_CARD = 0x50000000;

    // ------------------------------------------------------------------
    // Motion — lerp speeds (per second) + duration targets (ms)
    // ------------------------------------------------------------------
    public static final float MOTION_FAST = 18f;
    public static final float MOTION_NORMAL = 12f;
    public static final float MOTION_SLOW = 8f;

    public static final int DURATION_HOVER_MS = 80;
    public static final int DURATION_PRESS_MS = 60;
    public static final int DURATION_OPEN_MS = 140;
    public static final int DURATION_CLOSE_MS = 100;
    public static final int DURATION_TOOLTIP_MS = 2500;
    public static final int DURATION_TOAST_MS = 4000;

    /** When true, UI animations snap to their targets (accessibility). */
    public static volatile boolean reducedMotion = false;

    // ------------------------------------------------------------------
    // Typography (relative sizes — actual glyphs come from MC font)
    // ------------------------------------------------------------------
    public static final int TEXT_XS = 8;
    public static final int TEXT_SM = 9;
    public static final int TEXT_MD = 10;
    public static final int TEXT_LG = 12;

    // ------------------------------------------------------------------
    // Component sizes
    // ------------------------------------------------------------------
    public static final int ROW_HEIGHT = 16;
    public static final int TOGGLE_WIDTH = 22;
    public static final int TOGGLE_HEIGHT = 11;
    public static final int CHECKBOX_SIZE = 10;
    public static final int INPUT_HEIGHT = 14;
    public static final int BUTTON_HEIGHT_SM = 14;
    public static final int BUTTON_HEIGHT_MD = 18;
    public static final int BUTTON_HEIGHT_LG = 22;
    public static final int SLIDER_TRACK_H = 3;
    public static final int SLIDER_TRACK_H_COMPACT = 2;
    public static final int TAB_HEIGHT = 16;
    public static final int SEARCH_HEIGHT = 16;
    public static final int CARD_MIN_HEIGHT = 52;
    public static final int MENU_BUTTON_HEIGHT = 22;
    public static final int PANEL_WIDTH = 160;
    public static final int PANEL_HEADER = 18;
    public static final int DROPDOWN_ROW = 14;
    public static final int TOOLTIP_PAD_X = 6;
    public static final int TOOLTIP_PAD_Y = 4;

    // Color picker
    public static final int COLOR_SV_SIZE = 64;
    public static final int COLOR_HUE_W = 10;
    public static final int COLOR_ALPHA_H = 6;
    public static final int COLOR_PICKER_W = 120;
    public static final int COLOR_PICKER_H = 88;

    // HUD Editor V3
    public static final int GRID_SIZE = 8;
    public static final int SNAP_THRESHOLD = 4;
    public static final int EDITOR_HANDLE_SIZE = 5;
    public static final int EDITOR_INSPECTOR_HEIGHT = 62;
    public static final int EDITOR_POPOVER_W = 152;
    public static final int EDITOR_POPOVER_MAX_H = 220;

    // Notifications
    public static final int NOTIF_MAX_ACTIVE = 5;

    private PrimeDesign() {
    }

    /** Frame-rate independent lerp that snaps instantly when reduced motion is on. */
    public static float animate(float from, float to, float deltaSeconds, float speed) {
        if (reducedMotion) {
            return to;
        }
        float t = Math.clamp(deltaSeconds * speed, 0f, 1f);
        return from + (to - from) * t;
    }

    /**
     * Duration-based ease toward {@code to} using a target time in milliseconds
     * (hover/press/open/close). Equivalent to a critically-damped-ish step.
     */
    public static float animateMs(float from, float to, float deltaSeconds, int durationMs) {
        if (reducedMotion || durationMs <= 0) {
            return to;
        }
        float speed = 1000f / durationMs;
        return animate(from, to, deltaSeconds, speed);
    }

    /** Converts a [0,1] linear progress with ease-out cubic. */
    public static float easeOutCubic(float t) {
        t = Math.clamp(t, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }
}
