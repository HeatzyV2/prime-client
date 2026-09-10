package dev.primeclient.core.hud.elements;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.util.ColorUtil;

/** Smooth interpolation custom health bar HUD element. */
public final class CustomHealthHudElement extends HudElement {

    private final MinecraftAdapter adapter;
    private final ThemeManager themes;
    private float displayedHealth = 20f;

    public CustomHealthHudElement(MinecraftAdapter adapter, ThemeManager themes) {
        super("custom-health", "Custom Health", HudAnchor.BOTTOM_LEFT, 10, -35);
        this.adapter = adapter;
        this.themes = themes;
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return 130;
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        return 22;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        if (!adapter.hasPlayer()) {
            return;
        }

        float realHealth = adapter.playerHealth();
        float maxHealth = Math.max(1f, adapter.playerMaxHealth());

        // Smooth health drain interpolation
        displayedHealth += (realHealth - displayedHealth) * 0.15f;

        Theme theme = themes.active();
        int w = measureWidth(ctx);
        int h = measureHeight(ctx);
        int radius = PrimeDesign.RADIUS_SM;

        int fill = ColorUtil.withAlpha(0xFF0C0C0E, 0.9f);
        ctx.fillSoftShadow(0, 0, w, h, radius, 0x70000000);
        ctx.fillRoundedBorder(0, 0, w, h, radius, 1, ColorUtil.withAlpha(theme.accent(), 0.55f), fill);

        float pct = Math.min(1f, Math.max(0f, displayedHealth / maxHealth));
        int barW = Math.round((w - 8) * pct);
        int healthColor = ColorUtil.lerp(theme.error(), theme.success(), pct);

        if (barW > 0) {
            ctx.fillRoundedRect(4, 4, barW, h - 8, radius - 1, ColorUtil.withAlpha(healthColor, 0.85f));
            ctx.fillGradientVertical(4, 4, barW, (h - 8) / 2,
                    ColorUtil.withAlpha(0xFFFFFFFF, 0.25f), ColorUtil.withAlpha(0x00000000, 0f));
        }

        String label = String.format("%.0f / %.0f HP", realHealth, maxHealth);
        int textW = ctx.smoothTextWidth(label, 0.78f);
        ctx.drawSmoothText(label, (w - textW) / 2, (h - ctx.fontHeight()) / 2 + 1, theme.foreground(), 0.78f);
    }
}
