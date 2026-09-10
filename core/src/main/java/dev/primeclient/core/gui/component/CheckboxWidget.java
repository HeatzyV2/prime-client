package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/** Square checkbox with animated check fill. */
public final class CheckboxWidget {

    private float checkProgress;

    public void tick(boolean checked, float deltaSeconds) {
        checkProgress = PrimeDesign.animateMs(checkProgress, checked ? 1f : 0f,
                deltaSeconds, PrimeDesign.DURATION_PRESS_MS);
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, boolean checked, boolean hovered) {
        int s = PrimeDesign.CHECKBOX_SIZE;
        int border = hovered || checked ? theme.accent() : theme.border();
        ctx.fillRoundedBorder(x, y, s, s, PrimeDesign.RADIUS_SM, PrimeDesign.STROKE_THIN,
                border, ColorUtil.withAlpha(theme.backgroundLight(), 0.9f));
        if (checkProgress > 0.01f) {
            int inset = 2;
            int fill = Math.max(1, Math.round((s - inset * 2) * checkProgress));
            ctx.fillRoundedRect(x + inset, y + inset, fill, s - inset * 2,
                    PrimeDesign.RADIUS_SM, theme.accent());
        }
    }

    public boolean hit(double mx, double my, int x, int y) {
        int s = PrimeDesign.CHECKBOX_SIZE;
        return mx >= x && mx < x + s && my >= y && my < y + s;
    }
}
