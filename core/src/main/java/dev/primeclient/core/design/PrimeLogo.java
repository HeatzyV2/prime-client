package dev.primeclient.core.design;

import dev.primeclient.core.adapter.RenderContext;

/** Shared Prime logo asset and layout helpers. */
public final class PrimeLogo {

    public static final String TEXTURE = "textures/gui/logo.png";
    public static final int SRC_WIDTH = 1024;
    public static final int SRC_HEIGHT = 559;

    private PrimeLogo() {
    }

    public static int widthForHeight(int height) {
        return Math.max(1, Math.round(height * (SRC_WIDTH / (float) SRC_HEIGHT)));
    }

    public static void draw(RenderContext ctx, int x, int y, int height, int tintArgb) {
        int width = widthForHeight(height);
        ctx.drawTexture(TEXTURE, x, y, width, height, SRC_WIDTH, SRC_HEIGHT, tintArgb);
    }

    public static void drawCentered(RenderContext ctx, int centerX, int y, int height, int tintArgb) {
        draw(ctx, centerX - widthForHeight(height) / 2, y, height, tintArgb);
    }
}
