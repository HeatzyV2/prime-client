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
            drawUiText(text, x, y, argb);
            return;
        }
        pushTransform(x, y, scale);
        drawUiText(text, 0, 0, argb);
        popTransform();
    }

    default int smoothTextWidth(String text, float scale) {
        return Math.round(uiTextWidth(text) * scale);
    }

    /** Premium TTF UI labels (Inter). Falls back to vanilla in headless tests. */
    default void drawUiText(String text, int x, int y, int argb) {
        drawText(text, x, y, argb, false);
    }

    default int uiTextWidth(String text) {
        return textWidth(text);
    }

    default int uiFontHeight() {
        return fontHeight();
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

    /** Clips subsequent draw calls to the rectangle (screen space). No-op in headless tests. */
    default void pushClip(int x, int y, int width, int height) {
    }

    /** Ends the innermost clip region opened by {@link #pushClip(int, int, int, int)}. */
    default void popClip() {
    }

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

    /** Horizontal gradient fill (left → right). */
    default void fillGradientHorizontal(int x, int y, int width, int height, int leftArgb, int rightArgb) {
        if (width <= 0) {
            return;
        }
        for (int col = 0; col < width; col++) {
            float t = col / (float) Math.max(1, width - 1);
            int color = dev.primeclient.core.util.ColorUtil.lerp(leftArgb, rightArgb, t);
            fillRect(x + col, y, 1, height, color);
        }
    }

    default void fillRoundedRect(int x, int y, int width, int height, int radius, int argb) {
        dev.primeclient.core.gui.RoundedRect.fill(this, x, y, width, height, radius, argb);
    }

    default void fillRoundedBorder(int x, int y, int width, int height, int radius,
                                   int thickness, int borderArgb, int innerArgb) {
        dev.primeclient.core.gui.RoundedRect.border(this, x, y, width, height, radius, thickness, borderArgb, innerArgb);
    }

    default void fillSoftShadow(int x, int y, int width, int height, int radius, int shadowArgb) {
        dev.primeclient.core.gui.RoundedRect.softShadow(this, x, y, width, height, radius, shadowArgb);
    }
}
