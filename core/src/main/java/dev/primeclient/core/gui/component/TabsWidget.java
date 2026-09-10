package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

import java.util.List;

/** Horizontal tab strip with animated underline. */
public final class TabsWidget {

    private int selected;
    private float underlineProgress;

    public int selected() {
        return selected;
    }

    public void setSelected(int index) {
        if (index >= 0) {
            this.selected = index;
        }
    }

    public void tick(float deltaSeconds) {
        underlineProgress = PrimeDesign.animateMs(underlineProgress, 1f,
                deltaSeconds, PrimeDesign.DURATION_OPEN_MS);
    }

    /**
     * @return clicked tab index, or {@code -1}
     */
    public int mousePressed(double mx, double my, int x, int y, List<String> labels, int[] widths) {
        int cursor = x;
        for (int i = 0; i < labels.size(); i++) {
            int w = widths[i];
            if (mx >= cursor && mx < cursor + w && my >= y && my < y + PrimeDesign.TAB_HEIGHT) {
                if (selected != i) {
                    underlineProgress = 0f;
                    selected = i;
                }
                return i;
            }
            cursor += w + PrimeDesign.SPACE_SM;
        }
        return -1;
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, List<String> labels,
                       int[] widths, double mouseX, double mouseY) {
        int cursor = x;
        int h = PrimeDesign.TAB_HEIGHT;
        for (int i = 0; i < labels.size(); i++) {
            int w = widths[i];
            boolean active = i == selected;
            boolean hover = mouseX >= cursor && mouseX < cursor + w && mouseY >= y && mouseY < y + h;
            int color = active ? theme.foreground()
                    : hover ? theme.foreground() : theme.foregroundMuted();
            ctx.drawUiText(labels.get(i), cursor + PrimeDesign.SPACE_SM,
                    y + (h - ctx.uiFontHeight()) / 2, color);
            if (active) {
                int lineW = Math.max(8, Math.round((w - PrimeDesign.SPACE_SM * 2) * Math.max(0.2f, underlineProgress)));
                ctx.fillRect(cursor + PrimeDesign.SPACE_SM, y + h - 2, lineW, 2, theme.accent());
            } else if (hover) {
                ctx.fillRect(cursor + PrimeDesign.SPACE_SM, y + h - 1, w - PrimeDesign.SPACE_SM * 2, 1,
                        ColorUtil.withAlpha(theme.accent(), 0.45f));
            }
            cursor += w + PrimeDesign.SPACE_SM;
        }
    }

    public static int[] measure(RenderContext ctx, List<String> labels) {
        int[] widths = new int[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            widths[i] = ctx.uiTextWidth(labels.get(i)) + PrimeDesign.SPACE_SM * 2;
        }
        return widths;
    }
}
