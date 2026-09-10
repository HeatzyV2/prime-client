package dev.primeclient.core.gui;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/**
 * Lightweight tooltip overlay with optional detail line and fade-out.
 */
public final class TooltipRenderer {

    private String title = "";
    private String detail = "";
    private int x;
    private int y;
    private int showMillis;
    private float alpha = 1f;

    public void show(String text, int x, int y) {
        show(text, null, x, y);
    }

    public void show(String title, String detail, int x, int y) {
        this.title = title == null ? "" : title;
        this.detail = detail == null ? "" : detail;
        this.x = x;
        this.y = y;
        this.showMillis = PrimeDesign.DURATION_TOOLTIP_MS;
        this.alpha = 1f;
    }

    public void tick(int deltaMillis) {
        if (showMillis <= 0) {
            return;
        }
        showMillis -= deltaMillis;
        if (showMillis < 200) {
            alpha = Math.max(0f, showMillis / 200f);
        }
    }

    public void render(RenderContext ctx, Theme theme) {
        if (showMillis <= 0 || title.isEmpty() || alpha <= 0.01f) {
            return;
        }
        int pad = PrimeDesign.SPACE_SM + 2;
        int titleW = ctx.uiTextWidth(title);
        int detailW = detail.isEmpty() ? 0 : ctx.uiTextWidth(detail);
        int w = Math.max(titleW, detailW) + pad * 2;
        int lineH = ctx.uiFontHeight();
        int h = pad * 2 + lineH + (detail.isEmpty() ? 0 : lineH + 2);
        int bg = ColorUtil.withAlpha(theme.surfaceElevated(), 0.92f * alpha);
        int fg = ColorUtil.withAlpha(theme.foreground(), alpha);
        int muted = ColorUtil.withAlpha(theme.foregroundMuted(), alpha);
        ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_SM, bg);
        ctx.drawUiText(title, x + pad, y + pad, fg);
        if (!detail.isEmpty()) {
            ctx.drawUiText(detail, x + pad, y + pad + lineH + 2, muted);
        }
    }
}
