package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/**
 * Compact HSV color picker: hue bar + saturation/value field + alpha slider + hex preview.
 * SV field is drawn as horizontal gradient strips (O(height) draw calls, not O(width×height)).
 */
public final class ColorPickerWidget {

    public static final int WIDTH = 120;
    public static final int HEIGHT = 88;

    private float hue;
    private float saturation = 1f;
    private float value = 1f;
    private float alpha = 1f;
    private DragMode dragMode = DragMode.NONE;

    private enum DragMode {
        NONE, SV, HUE, ALPHA
    }

    public void load(int argb) {
        float[] hsv = ColorUtil.toHsv(argb);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        alpha = hsv[3];
    }

    public int selectedArgb() {
        return ColorUtil.fromHsv(hue, saturation, value, alpha);
    }

    public void render(RenderContext ctx, Theme theme, int x, int y) {
        int svSize = 64;
        int svX = x;
        int svY = y;
        int hueX = x + svSize + PrimeDesign.SPACE_SM;
        int hueY = y;
        int hueW = 10;
        int hueH = svSize;
        int alphaY = y + svSize + PrimeDesign.SPACE_SM;
        int previewX = hueX;
        int previewY = alphaY;

        // SV field: one horizontal gradient per row (white→hue at value v).
        for (int py = 0; py < svSize; py++) {
            float v = 1f - py / (float) svSize;
            int left = ColorUtil.fromHsv(hue, 0f, v, 1f);
            int right = ColorUtil.fromHsv(hue, 1f, v, 1f);
            ctx.fillGradientHorizontal(svX, svY + py, svSize, 1, left, right);
        }
        int cx = svX + Math.round(saturation * svSize);
        int cy = svY + Math.round((1f - value) * svSize);
        ctx.fillRect(cx - 1, cy - 1, 3, 3, theme.foreground());

        // Hue bar — chunked vertical strips (6° steps ≈ 60 fills).
        int hueSteps = 60;
        for (int i = 0; i < hueSteps; i++) {
            float h0 = i / (float) hueSteps * 360f;
            float h1 = (i + 1) / (float) hueSteps * 360f;
            int y0 = hueY + i * hueH / hueSteps;
            int y1 = hueY + (i + 1) * hueH / hueSteps;
            ctx.fillGradientVertical(hueX, y0, hueW, Math.max(1, y1 - y0),
                    ColorUtil.fromHsv(h0, 1f, 1f, 1f),
                    ColorUtil.fromHsv(h1, 1f, 1f, 1f));
        }
        int hy = hueY + Math.round(hue / 360f * hueH);
        ctx.fillRect(hueX - 1, hy, hueW + 2, 2, theme.foreground());

        // Alpha bar — single horizontal gradient.
        int alphaW = svSize + hueW + PrimeDesign.SPACE_SM;
        ctx.fillGradientHorizontal(x, alphaY, alphaW, 6,
                ColorUtil.fromHsv(hue, saturation, value, 0f),
                ColorUtil.fromHsv(hue, saturation, value, 1f));
        int ax = x + Math.round(alpha * alphaW);
        ctx.fillRect(ax, alphaY - 1, 2, 8, theme.foreground());

        ctx.fillRect(previewX, previewY, 18, 12, selectedArgb());
        GuiLayout.label(ctx, ColorUtil.toHex(selectedArgb()), previewX + 22, previewY + 2, theme.foregroundMuted());
    }

    public boolean mousePressed(double mx, double my, int x, int y, int button) {
        if (button != 0) {
            return false;
        }
        int svSize = 64;
        if (mx >= x && mx < x + svSize && my >= y && my < y + svSize) {
            dragMode = DragMode.SV;
            updateSv(mx, my, x, y, svSize);
            return true;
        }
        int hueX = x + svSize + PrimeDesign.SPACE_SM;
        if (mx >= hueX && mx < hueX + 10 && my >= y && my < y + svSize) {
            dragMode = DragMode.HUE;
            updateHue(my, y, svSize);
            return true;
        }
        int alphaY = y + svSize + PrimeDesign.SPACE_SM;
        int alphaW = svSize + 10 + PrimeDesign.SPACE_SM;
        if (mx >= x && mx < x + alphaW && my >= alphaY && my < alphaY + 6) {
            dragMode = DragMode.ALPHA;
            alpha = Math.clamp((float) (mx - x) / alphaW, 0f, 1f);
            return true;
        }
        return false;
    }

    public void mouseDragged(double mx, double my, int x, int y) {
        int svSize = 64;
        switch (dragMode) {
            case SV -> updateSv(mx, my, x, y, svSize);
            case HUE -> updateHue(my, y, svSize);
            case ALPHA -> {
                int alphaW = svSize + 10 + PrimeDesign.SPACE_SM;
                alpha = Math.clamp((float) (mx - x) / alphaW, 0f, 1f);
            }
            default -> {
            }
        }
    }

    public void mouseReleased() {
        dragMode = DragMode.NONE;
    }

    public boolean hit(double mx, double my, int x, int y) {
        return mx >= x && mx < x + WIDTH && my >= y && my < y + HEIGHT;
    }

    private void updateSv(double mx, double my, int x, int y, int svSize) {
        saturation = Math.clamp((float) (mx - x) / svSize, 0f, 1f);
        value = 1f - Math.clamp((float) (my - y) / svSize, 0f, 1f);
    }

    private void updateHue(double my, int y, int svSize) {
        hue = Math.clamp((float) (my - y) / svSize, 0f, 1f) * 360f;
    }
}
