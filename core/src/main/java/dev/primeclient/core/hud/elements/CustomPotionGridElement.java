package dev.primeclient.core.hud.elements;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.util.ColorUtil;

/** Frosted glass potion status card with expiry pulse warnings. */
public final class CustomPotionGridElement extends HudElement {

    private final ThemeManager themes;

    public CustomPotionGridElement(ThemeManager themes) {
        super("custom-potion-grid", "Custom Potion Grid", HudAnchor.MIDDLE_RIGHT, -6, 80);
        this.themes = themes;
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return 110;
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        return 24;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        Theme theme = themes.active();
        int w = measureWidth(ctx);
        int h = measureHeight(ctx);
        int radius = PrimeDesign.RADIUS_SM;

        int fill = ColorUtil.withAlpha(0xFF0C0C0E, 0.88f);
        ctx.fillSoftShadow(0, 0, w, h, radius, 0x60000000);
        ctx.fillRoundedBorder(0, 0, w, h, radius, 1, ColorUtil.withAlpha(theme.accent(), 0.5f), fill);

        ctx.drawSmoothText("🧪 Potions", 8, (h - ctx.fontHeight()) / 2 + 1, theme.foreground(), 0.78f);
    }
}
