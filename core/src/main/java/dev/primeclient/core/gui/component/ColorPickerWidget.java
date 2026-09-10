package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/**
 * Compact HSV color picker: hue bar + saturation/value field + alpha slider + hex.
 * SV field is drawn as horizontal gradient strips (O(height) draw calls).
 *
 * <p>HEX: click the hex label to focus editing; type digits; Enter commits;
 * Escape cancels. {@link #applyHex(String)} accepts {@code #RGB}, {@code #RRGGBB},
 * {@code #AARRGGBB}.</p>
 */
public final class ColorPickerWidget {

    public static final int WIDTH = PrimeDesign.COLOR_PICKER_W;
    public static final int HEIGHT = PrimeDesign.COLOR_PICKER_H;

    private float hue;
    private float saturation = 1f;
    private float value = 1f;
    private float alpha = 1f;
    private DragMode dragMode = DragMode.NONE;

    private boolean hexEditing;
    private final StringBuilder hexBuffer = new StringBuilder(9);
    private int hexX;
    private int hexY;
    private int hexW;
    private int hexH;

    private enum DragMode {
        NONE, SV, HUE, ALPHA
    }

    public void load(int argb) {
        float[] hsv = ColorUtil.toHsv(argb);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        alpha = hsv[3];
        hexEditing = false;
        hexBuffer.setLength(0);
    }

    public int selectedArgb() {
        return ColorUtil.fromHsv(hue, saturation, value, alpha);
    }

    public String hex() {
        return ColorUtil.toHex(selectedArgb());
    }

    public boolean applyHex(String text) {
        if (!ColorUtil.isHex(text)) {
            return false;
        }
        load(ColorUtil.parseHex(text, selectedArgb()));
        return true;
    }

    public boolean hexEditing() {
        return hexEditing;
    }

    public void render(RenderContext ctx, Theme theme, int x, int y) {
        int svSize = PrimeDesign.COLOR_SV_SIZE;
        int svX = x;
        int svY = y;
        int hueX = x + svSize + PrimeDesign.SPACE_SM;
        int hueY = y;
        int hueW = PrimeDesign.COLOR_HUE_W;
        int hueH = svSize;
        int alphaY = y + svSize + PrimeDesign.SPACE_SM;
        int previewX = hueX;
        int previewY = alphaY;

        for (int py = 0; py < svSize; py++) {
            float v = 1f - py / (float) svSize;
            int left = ColorUtil.fromHsv(hue, 0f, v, 1f);
            int right = ColorUtil.fromHsv(hue, 1f, v, 1f);
            ctx.fillGradientHorizontal(svX, svY + py, svSize, 1, left, right);
        }
        int cx = svX + Math.round(saturation * svSize);
        int cy = svY + Math.round((1f - value) * svSize);
        ctx.fillRect(cx - 1, cy - 1, 3, 3, theme.foreground());

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

        int alphaW = svSize + hueW + PrimeDesign.SPACE_SM;
        ctx.fillGradientHorizontal(x, alphaY, alphaW, PrimeDesign.COLOR_ALPHA_H,
                ColorUtil.fromHsv(hue, saturation, value, 0f),
                ColorUtil.fromHsv(hue, saturation, value, 1f));
        int ax = x + Math.round(alpha * alphaW);
        ctx.fillRect(ax, alphaY - 1, 2, PrimeDesign.COLOR_ALPHA_H + 2, theme.foreground());

        ctx.fillRect(previewX, previewY, 18, 12, selectedArgb());
        String hexLabel = hexEditing ? hexBuffer.toString() : ColorUtil.toHex(selectedArgb());
        if (hexEditing && (System.currentTimeMillis() / 500) % 2 == 0) {
            hexLabel = hexLabel + "_";
        }
        hexX = previewX + 22;
        hexY = previewY + 2;
        hexW = Math.max(48, ctx.uiTextWidth(hexLabel) + 4);
        hexH = ctx.uiFontHeight() + 2;
        if (hexEditing) {
            ctx.fillRoundedRect(hexX - 2, hexY - 1, hexW, hexH, PrimeDesign.RADIUS_SM,
                    ColorUtil.withAlpha(theme.backgroundLight(), 0.95f));
        }
        GuiLayout.label(ctx, hexLabel, hexX, hexY,
                hexEditing ? theme.accent() : theme.foregroundMuted());
    }

    public boolean mousePressed(double mx, double my, int x, int y, int button) {
        if (button != 0) {
            return false;
        }
        if (hexEditing && mx >= hexX - 2 && mx < hexX + hexW && my >= hexY - 1 && my < hexY + hexH) {
            return true;
        }
        if (!hexEditing && mx >= hexX - 2 && mx < hexX + hexW && my >= hexY - 1 && my < hexY + hexH) {
            beginHexEdit();
            return true;
        }
        if (hexEditing) {
            commitHexEdit();
        }
        int svSize = PrimeDesign.COLOR_SV_SIZE;
        if (mx >= x && mx < x + svSize && my >= y && my < y + svSize) {
            dragMode = DragMode.SV;
            updateSv(mx, my, x, y, svSize);
            return true;
        }
        int hueX = x + svSize + PrimeDesign.SPACE_SM;
        if (mx >= hueX && mx < hueX + PrimeDesign.COLOR_HUE_W && my >= y && my < y + svSize) {
            dragMode = DragMode.HUE;
            updateHue(my, y, svSize);
            return true;
        }
        int alphaY = y + svSize + PrimeDesign.SPACE_SM;
        int alphaW = svSize + PrimeDesign.COLOR_HUE_W + PrimeDesign.SPACE_SM;
        if (mx >= x && mx < x + alphaW && my >= alphaY && my < alphaY + PrimeDesign.COLOR_ALPHA_H) {
            dragMode = DragMode.ALPHA;
            alpha = Math.clamp((float) (mx - x) / alphaW, 0f, 1f);
            return true;
        }
        return false;
    }

    public void mouseDragged(double mx, double my, int x, int y) {
        int svSize = PrimeDesign.COLOR_SV_SIZE;
        switch (dragMode) {
            case SV -> updateSv(mx, my, x, y, svSize);
            case HUE -> updateHue(my, y, svSize);
            case ALPHA -> {
                int alphaW = svSize + PrimeDesign.COLOR_HUE_W + PrimeDesign.SPACE_SM;
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

    public boolean charTyped(char c) {
        if (!hexEditing) {
            return false;
        }
        if (c == '#' && hexBuffer.isEmpty()) {
            hexBuffer.append('#');
            return true;
        }
        if (isHexChar(c) && hexBuffer.length() < 9) {
            if (hexBuffer.isEmpty()) {
                hexBuffer.append('#');
            }
            hexBuffer.append(Character.toUpperCase(c));
            return true;
        }
        return false;
    }

    public boolean keyPressed(int glfwKey) {
        if (!hexEditing) {
            return false;
        }
        if (glfwKey == 257) { // Enter
            commitHexEdit();
            return true;
        }
        if (glfwKey == 256) { // Escape
            hexEditing = false;
            hexBuffer.setLength(0);
            return true;
        }
        if (glfwKey == 259 && !hexBuffer.isEmpty()) { // Backspace
            hexBuffer.deleteCharAt(hexBuffer.length() - 1);
            return true;
        }
        return false;
    }

    private void beginHexEdit() {
        hexEditing = true;
        hexBuffer.setLength(0);
        hexBuffer.append(ColorUtil.toHex(selectedArgb()));
    }

    private void commitHexEdit() {
        if (hexEditing && hexBuffer.length() > 1) {
            applyHex(hexBuffer.toString());
        }
        hexEditing = false;
        hexBuffer.setLength(0);
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private void updateSv(double mx, double my, int x, int y, int svSize) {
        saturation = Math.clamp((float) (mx - x) / svSize, 0f, 1f);
        value = 1f - Math.clamp((float) (my - y) / svSize, 0f, 1f);
    }

    private void updateHue(double my, int y, int svSize) {
        hue = Math.clamp((float) (my - y) / svSize, 0f, 1f) * 360f;
    }
}
