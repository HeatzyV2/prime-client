package dev.primeclient.core.state;

/** Client-side cosmetic overrides, read by version-layer render hooks. */
public final class CosmeticsState {

    public enum CapeStyle {
        NONE,
        PRIME,
        STAR
    }

    private static CapeStyle capeStyle = CapeStyle.NONE;
    private static int accentTint;

    private CosmeticsState() {
    }

    public static CapeStyle capeStyle() {
        return capeStyle;
    }

    public static int accentTint() {
        return accentTint;
    }

    public static void setCapeStyle(CapeStyle style) {
        capeStyle = style != null ? style : CapeStyle.NONE;
    }

    public static void setAccentTint(int tintArgb) {
        accentTint = tintArgb;
    }

    public static void reset() {
        capeStyle = CapeStyle.NONE;
        accentTint = 0;
    }
}
