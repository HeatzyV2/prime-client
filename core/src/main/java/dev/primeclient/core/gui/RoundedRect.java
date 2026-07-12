package dev.primeclient.core.gui;

import dev.primeclient.core.adapter.RenderContext;

/** Software rounded-rectangle fills (Feather-style panels without a shader). */
public final class RoundedRect {

    private RoundedRect() {
    }

    public static void fill(RenderContext ctx, int x, int y, int width, int height, int radius, int argb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (radius <= 0) {
            ctx.fillRect(x, y, width, height, argb);
            return;
        }
        radius = Math.min(radius, Math.min(width, height) / 2);
        ctx.fillRect(x + radius, y, width - radius * 2, height, argb);
        ctx.fillRect(x, y + radius, width, height - radius * 2, argb);
        fillCorner(ctx, x, y, radius, argb, true, true);
        fillCorner(ctx, x + width - radius, y, radius, argb, false, true);
        fillCorner(ctx, x, y + height - radius, radius, argb, true, false);
        fillCorner(ctx, x + width - radius, y + height - radius, radius, argb, false, false);
    }

    /** Outer rounded rect minus inner — cheap 1px border. */
    public static void border(RenderContext ctx, int x, int y, int width, int height,
                              int radius, int thickness, int borderArgb, int innerArgb) {
        fill(ctx, x, y, width, height, radius, borderArgb);
        fill(ctx, x + thickness, y + thickness,
                width - thickness * 2, height - thickness * 2,
                Math.max(0, radius - thickness), innerArgb);
    }

    public static void softShadow(RenderContext ctx, int x, int y, int width, int height,
                                  int radius, int shadowArgb) {
        int baseAlpha = (shadowArgb >>> 24) & 0xFF;
        if (baseAlpha <= 0) {
            return;
        }
        for (int spread = 5; spread >= 1; spread--) {
            float falloff = 1f - spread / 6f;
            int alpha = Math.round(baseAlpha * falloff * 0.35f);
            if (alpha <= 0) {
                continue;
            }
            int color = (alpha << 24) | (shadowArgb & 0x00FFFFFF);
            fill(ctx, x - spread, y - spread + 2, width + spread * 2, height + spread * 2,
                    radius + 1, color);
        }
    }

    private static void fillCorner(RenderContext ctx, int left, int top, int radius, int argb,
                                   boolean leftArc, boolean topArc) {
        for (int dy = 0; dy < radius; dy++) {
            for (int dx = 0; dx < radius; dx++) {
                float px = leftArc ? (radius - dx - 0.5f) : (dx + 0.5f);
                float py = topArc ? (radius - dy - 0.5f) : (dy + 0.5f);
                if (px * px + py * py > radius * radius) {
                    continue;
                }
                ctx.fillRect(left + dx, top + dy, 1, 1, argb);
            }
        }
    }
}
