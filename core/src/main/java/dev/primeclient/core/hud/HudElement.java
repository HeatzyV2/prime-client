package dev.primeclient.core.hud;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.i18n.PrimeLang;

/**
 * One movable HUD component (FPS counter, keystrokes, notifications, ...).
 */
public abstract class HudElement {

    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 3.0f;
    public static final float MIN_OPACITY = 0.1f;
    public static final float MAX_OPACITY = 1.0f;

    private final String id;
    private final String name;
    private final HudAnchor defaultAnchor;
    private final float defaultOffsetX;
    private final float defaultOffsetY;

    private HudAnchor anchor;
    private float offsetX;
    private float offsetY;
    private float scale = 1.0f;
    private float rotation;
    private float opacity = 1.0f;
    private int tintArgb;
    /** User layout preference (persisted). Hidden elements stay configurable in the editor list. */
    private boolean visible = true;
    /**
     * Module gate (not persisted). When {@code false}, the element is not drawn in-game and
     * is not interactive on the editor canvas — even if {@link #visible} is true.
     */
    private boolean active = true;
    private boolean locked;

    private float lastX;
    private float lastY;
    private float lastWidth;
    private float lastHeight;

    protected HudElement(String id, String name, HudAnchor defaultAnchor, float defaultOffsetX, float defaultOffsetY) {
        this.id = id;
        this.name = name;
        this.defaultAnchor = defaultAnchor;
        this.defaultOffsetX = defaultOffsetX;
        this.defaultOffsetY = defaultOffsetY;
        this.anchor = defaultAnchor;
        this.offsetX = defaultOffsetX;
        this.offsetY = defaultOffsetY;
    }

    /** Restores the constructor-time layout plus neutral scale, rotation, opacity and tint. */
    public final void resetToDefaults() {
        this.anchor = defaultAnchor;
        this.offsetX = defaultOffsetX;
        this.offsetY = defaultOffsetY;
        this.scale = 1.0f;
        this.rotation = 0f;
        this.opacity = 1.0f;
        this.tintArgb = 0;
        this.visible = true;
        // {@code active} is owned by the binding module — reset leaves it alone.
        this.locked = false;
    }

    public abstract int measureWidth(RenderContext ctx);

    public abstract int measureHeight(RenderContext ctx);

    public abstract void render(RenderContext ctx, long nowMillis);

    public final String id() {
        return id;
    }

    public final String name() {
        return PrimeLang.hud(id, name);
    }

    public final HudAnchor anchor() {
        return anchor;
    }

    public final float offsetX() {
        return offsetX;
    }

    public final float offsetY() {
        return offsetY;
    }

    public final float scale() {
        return scale;
    }

    public final float rotation() {
        return rotation;
    }

    public final float opacity() {
        return opacity;
    }

    public final int tintArgb() {
        return tintArgb;
    }

    public final boolean isVisible() {
        return visible;
    }

    public final void setVisible(boolean visible) {
        this.visible = visible;
    }

    public final boolean isActive() {
        return active;
    }

    public final void setActive(boolean active) {
        this.active = active;
    }

    /** In-game draw + canvas pick: layout-visible and module-enabled. */
    public final boolean isShown() {
        return visible && active;
    }

    public final boolean isLocked() {
        return locked;
    }

    public final void setLocked(boolean locked) {
        this.locked = locked;
    }

    public final void setLayout(HudAnchor anchor, float offsetX, float offsetY) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public final void setScale(float scale) {
        this.scale = Math.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public final void setRotation(float rotation) {
        this.rotation = rotation % 360f;
    }

    public final void setOpacity(float opacity) {
        this.opacity = Math.clamp(opacity, MIN_OPACITY, MAX_OPACITY);
    }

    public final void setTintArgb(int tintArgb) {
        this.tintArgb = tintArgb;
    }

    final void setLastBounds(float x, float y, float width, float height) {
        this.lastX = x;
        this.lastY = y;
        this.lastWidth = width;
        this.lastHeight = height;
    }

    public final float lastX() {
        return lastX;
    }

    public final float lastY() {
        return lastY;
    }

    public final float lastWidth() {
        return lastWidth;
    }

    public final float lastHeight() {
        return lastHeight;
    }

    /**
     * Hit-test against the axis-aligned bounds of the unrotated box, or the
     * rotated OBB when rotation is applied (inverse-rotate the cursor around the center).
     */
    public final boolean containsPoint(double x, double y) {
        if (lastWidth <= 0 || lastHeight <= 0) {
            return false;
        }
        float cx = lastX + lastWidth / 2f;
        float cy = lastY + lastHeight / 2f;
        double lx = x - cx;
        double ly = y - cy;
        float rot = rotation;
        if (rot != 0f) {
            double rad = Math.toRadians(-rot);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double rx = lx * cos - ly * sin;
            double ry = lx * sin + ly * cos;
            lx = rx;
            ly = ry;
        }
        return lx >= -lastWidth / 2f && lx < lastWidth / 2f
                && ly >= -lastHeight / 2f && ly < lastHeight / 2f;
    }
}
