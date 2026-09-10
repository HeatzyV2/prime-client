package dev.primeclient.core.theme;

/**
 * A Prime Client color theme. Colors are packed ARGB ints (0xAARRGGBB) —
 * the format every renderer consumes directly, no conversion in render paths.
 */
public record Theme(
        String id,
        String name,
        int accent,
        int accentSecondary,
        int background,
        int backgroundLight,
        int surfaceElevated,
        int foreground,
        int foregroundMuted,
        int border,
        int overlay,
        int gradientTop,
        int gradientBottom,
        int success,
        int warning,
        int error
) {
    /** Returns an animated RGB shift / wave color phase shift without object allocations. */
    public int animatedAccent(float timeSeconds) {
        float wave = 0.5f + 0.5f * (float) Math.sin(timeSeconds * 2.5f);
        return dev.primeclient.core.util.ColorUtil.lerp(accent, accentSecondary, wave);
    }
}
