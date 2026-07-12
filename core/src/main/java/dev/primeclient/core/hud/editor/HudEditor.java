package dev.primeclient.core.hud.editor;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;

/**
 * HUD editor: selection, drag & drop, scale, rotation, opacity and tint.
 */
public final class HudEditor {

    private static final int[] TINT_PRESETS = {
            0,
            0xFFFFFFFF,
            0xFFFF5555,
            0xFF55FF55,
            0xFF5555FF,
            0xFFFFFF55
    };

    private final HudManager hud;
    private final ThemeManager themes;

    private HudElement selected;
    private HudElement dragging;
    private float grabOffsetX;
    private float grabOffsetY;
    private boolean snapEnabled = false;
    private boolean showGrid = true;

    private int tintPresetIndex;

    public HudEditor(HudManager hud, ThemeManager themes) {
        this.hud = hud;
        this.themes = themes;
    }

    public HudElement selected() {
        return selected;
    }

    public boolean mousePressed(double mouseX, double mouseY) {
        HudElement hit = hud.elementAt(mouseX, mouseY);
        this.selected = hit;
        this.dragging = hit;
        if (hit != null) {
            this.grabOffsetX = (float) (mouseX - hit.lastX());
            this.grabOffsetY = (float) (mouseY - hit.lastY());
            return true;
        }
        return false;
    }

    public void mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        HudElement element = dragging;
        if (element == null) {
            return;
        }
        float width = element.lastWidth();
        float height = element.lastHeight();
        float x = Math.clamp((float) mouseX - grabOffsetX, 0, screenWidth - width);
        float y = Math.clamp((float) mouseY - grabOffsetY, 0, screenHeight - height);
        if (snapEnabled) {
            x = snap(x);
            y = snap(y);
        }
        HudAnchor anchor = HudAnchor.closest(
                (x + width / 2f) / screenWidth,
                (y + height / 2f) / screenHeight);
        element.setLayout(anchor, x - anchor.baseX(screenWidth, width), y - anchor.baseY(screenHeight, height));
    }

    public void mouseReleased() {
        this.dragging = null;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta, boolean shiftDown, boolean ctrlDown) {
        HudElement target = selected != null && selected.containsPoint(mouseX, mouseY)
                ? selected
                : hud.elementAt(mouseX, mouseY);
        if (target == null) {
            return false;
        }
        float step = (float) Math.signum(scrollDelta);
        if (shiftDown) {
            target.setRotation(target.rotation() + step * 5f);
            return true;
        }
        if (ctrlDown) {
            target.setOpacity(target.opacity() + step * 0.05f);
            return true;
        }
        target.setScale(target.scale() + step * 0.1f);
        return true;
    }

    /** R cycles tint; G toggles grid. */
    public boolean keyPressed(int glfwKey) {
        if (glfwKey == 71) {
            showGrid = !showGrid;
            return true;
        }
        if (selected == null) {
            return false;
        }
        if (glfwKey == 82) {
            tintPresetIndex = (tintPresetIndex + 1) % TINT_PRESETS.length;
            selected.setTintArgb(TINT_PRESETS[tintPresetIndex]);
            return true;
        }
        return false;
    }

    public void renderOverlay(RenderContext ctx, double mouseX, double mouseY) {
        Theme theme = themes.active();
        if (showGrid) {
            drawGrid(ctx, theme);
        }
        for (HudElement element : hud.all()) {
            if (!element.isVisible()) {
                continue;
            }
            boolean isSelected = element == selected;
            boolean isHovered = element.containsPoint(mouseX, mouseY);
            if (isSelected || isHovered) {
                int color = isSelected ? theme.accent() : theme.foregroundMuted();
                drawBorder(ctx,
                        Math.round(element.lastX()) - 1,
                        Math.round(element.lastY()) - 1,
                        Math.round(element.lastWidth()) + 2,
                        Math.round(element.lastHeight()) + 2,
                        color);
            }
        }
        if (selected != null) {
            String props = String.format("%s — Scale %.1fx  Rot %.0f°  Opacity %.0f%%",
                    selected.name(), selected.scale(), selected.rotation(), selected.opacity() * 100f);
            ctx.drawText(props, 8, ctx.screenHeight() - 36, theme.foreground(), true);
        }
    }

    private static void drawBorder(RenderContext ctx, int x, int y, int width, int height, int argb) {
        ctx.fillRect(x, y, width, 1, argb);
        ctx.fillRect(x, y + height - 1, width, 1, argb);
        ctx.fillRect(x, y + 1, 1, height - 2, argb);
        ctx.fillRect(x + width - 1, y + 1, 1, height - 2, argb);
    }

    private static void drawGrid(RenderContext ctx, Theme theme) {
        int grid = PrimeDesign.GRID_SIZE * 2;
        int color = theme.border() & 0x40FFFFFF;
        for (int x = 0; x < ctx.screenWidth(); x += grid) {
            ctx.fillRect(x, 0, 1, ctx.screenHeight(), color);
        }
        for (int y = 0; y < ctx.screenHeight(); y += grid) {
            ctx.fillRect(0, y, ctx.screenWidth(), 1, color);
        }
    }

    private static float snap(float value) {
        return Math.round(value / PrimeDesign.GRID_SIZE) * PrimeDesign.GRID_SIZE;
    }
}
