package dev.primeclient.core.hud.elements;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.util.ColorUtil;

/** Frosted glass pill overhaul for Bossbars (Ender Dragon, Wither, Raids). */
public final class CustomBossbarElement extends HudElement {

    private final ThemeManager themes;
    private String bossTitle = "BOSS";
    private float healthPercent = 1f;

    public CustomBossbarElement(ThemeManager themes) {
        super("custom-bossbar", "Custom Bossbar", HudAnchor.TOP_CENTER, 0, 8);
        this.themes = themes;
    }

    public void updateBoss(String title, float percent) {
        this.bossTitle = title;
        this.healthPercent = Math.min(1f, Math.max(0f, percent));
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return 180;
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        return 22;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        Theme theme = themes.active();
        int w = measureWidth(ctx);
        int h = measureHeight(ctx);
        int radius = h / 2;

        int fill = ColorUtil.withAlpha(0xFF0C0C0E, 0.92f);
        ctx.fillSoftShadow(0, 0, w, h, radius, 0x80000000);
        ctx.fillRoundedBorder(0, 0, w, h, radius, 1, ColorUtil.withAlpha(theme.accent(), 0.6f), fill);

        int barW = Math.max(0, Math.round((w - 12) * healthPercent));
        if (barW > 0) {
            ctx.fillRoundedRect(6, 4, barW, h - 8, (h - 8) / 2, ColorUtil.withAlpha(theme.accent(), 0.85f));
            ctx.fillGradientVertical(6, 4, barW, (h - 8) / 2,
                    ColorUtil.withAlpha(0xFFFFFFFF, 0.22f), ColorUtil.withAlpha(0x00000000, 0f));
        }

        int textW = ctx.smoothTextWidth(bossTitle, 0.78f);
        ctx.drawSmoothText(bossTitle, (w - textW) / 2, (h - ctx.fontHeight()) / 2 + 1, theme.foreground(), 0.78f);
    }
}
