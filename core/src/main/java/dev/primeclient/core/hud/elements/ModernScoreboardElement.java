package dev.primeclient.core.hud.elements;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.util.ColorUtil;

/** Redesigned modern scoreboard HUD card with glass styling and smooth text. */
public final class ModernScoreboardElement extends HudElement {

    private final MinecraftAdapter adapter;
    private final ThemeManager themes;

    public ModernScoreboardElement(MinecraftAdapter adapter, ThemeManager themes) {
        super("modern-scoreboard", "Modern Scoreboard", HudAnchor.MIDDLE_RIGHT, -6, 0);
        this.adapter = adapter;
        this.themes = themes;
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        int maxW = 120;
        String title = adapter.scoreboardTitle();
        if (!title.isEmpty()) {
            maxW = Math.max(maxW, ctx.smoothTextWidth(title, 0.85f) + 24);
        }
        int count = adapter.scoreboardLineCount();
        for (int i = 0; i < count; i++) {
            String line = adapter.scoreboardLine(i);
            if (!line.isEmpty()) {
                maxW = Math.max(maxW, ctx.smoothTextWidth(line, 0.78f) + 24);
            }
        }
        return Math.min(220, maxW);
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        int count = adapter.scoreboardLineCount();
        if (count == 0 && adapter.scoreboardTitle().isEmpty()) {
            return 0;
        }
        return 22 + count * 14 + 6;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        int count = adapter.scoreboardLineCount();
        String title = adapter.scoreboardTitle();
        if (count == 0 && title.isEmpty()) {
            return;
        }

        Theme theme = themes.active();
        int w = measureWidth(ctx);
        int h = measureHeight(ctx);
        int radius = PrimeDesign.RADIUS_SM;

        int fill = ColorUtil.withAlpha(0xFF0C0C0E, 0.88f);
        ctx.fillSoftShadow(0, 0, w, h, radius, 0x70000000);
        ctx.fillRoundedBorder(0, 0, w, h, radius, 1, ColorUtil.withAlpha(theme.accent(), 0.5f), fill);

        // Accent top bar
        ctx.fillRect(4, 0, w - 8, 2, theme.accent());

        // Title
        if (!title.isEmpty()) {
            ctx.drawSmoothText(title, 10, 5, theme.foreground(), 0.85f);
        }

        // Content lines
        int y = 20;
        for (int i = 0; i < count; i++) {
            String line = adapter.scoreboardLine(i);
            if (!line.isEmpty()) {
                ctx.drawSmoothText(line, 10, y, theme.foregroundMuted(), 0.78f);
                y += 14;
            }
        }
    }
}
