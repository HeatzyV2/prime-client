package dev.primeclient.core.adapter;

/**
 * Version-independent 2D drawing surface for HUD and GUI rendering.
 *
 * <p>Backed by {@code GuiGraphics} on 1.21.11 and {@code GuiGraphicsExtractor}
 * on 26.2. One instance is reused every frame — implementations must be
 * stateless between {@code prepare} calls and allocation-free during drawing
 * (except while GUI screens are open).</p>
 */
public interface RenderContext {

    int screenWidth();

    int screenHeight();

    void fillRect(int x, int y, int width, int height, int argb);

    void drawText(String text, int x, int y, int argb, boolean shadow);

    /** Shadowless text with optional uniform scale — cleaner than vanilla MC labels. */
    default void drawSmoothText(String text, int x, int y, int argb, float scale) {
        if (Math.abs(scale - 1f) < 0.01f) {
            drawText(text, x, y, argb, false);
            return;
        }
        pushTransform(x, y, scale);
        drawText(text, 0, 0, argb, false);
        popTransform();
    }

    default int smoothTextWidth(String text, float scale) {
        return Math.round(textWidth(text) * scale);
    }

    int textWidth(String text);

    int fontHeight();

    /** Pushes translation + uniform scale around local origin. */
    default void pushTransform(float translateX, float translateY, float scale) {
        pushTransform(translateX, translateY, scale, 0f, 0f, 0f);
    }

    /**
     * Pushes translation, rotation (degrees) and scale around a local pivot.
     * Draw calls after this should use coordinates relative to the pivot.
     */
    void pushTransform(float translateX, float translateY, float scale,
                       float rotationDegrees, float pivotLocalX, float pivotLocalY);

    void popTransform();

    /** Multiplies alpha on subsequent draw calls until the next {@link #setDrawOpacity(float)}. */
    void setDrawOpacity(float opacity);

    /**
     * Draws a mod GUI texture. {@code texturePath} is under {@code assets/primeclient/},
     * e.g. {@code textures/gui/logo.png}. Default is a no-op for headless tests.
     */
    default void drawTexture(String texturePath, int x, int y, int width, int height,
                             int textureWidth, int textureHeight, int tintArgb) {
    }

    /** Vertical gradient fill (top → bottom). Default uses scanline lerp. */
    default void fillGradientVertical(int x, int y, int width, int height, int topArgb, int bottomArgb) {
        if (height <= 0) {
            return;
        }
        for (int row = 0; row < height; row++) {
            float t = row / (float) Math.max(1, height - 1);
            int color = dev.primeclient.core.util.ColorUtil.lerp(topArgb, bottomArgb, t);
            fillRect(x, y + row, width, 1, color);
        }
    }
}
