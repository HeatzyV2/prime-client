package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeLogo;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;
import dev.primeclient.core.util.Easing;

/**
 * Prime pause menu — soft dark glass, rounded chrome, centered GAME MENU panel.
 */
public final class GameMenuRenderer {

    /** Soft radii used only for pause chrome (premium, not tactical-sharp). */
    private static final int PANEL_RADIUS = 10;
    private static final int BTN_RADIUS = 7;

    private static final GameMenuAction[] GRID = {
            GameMenuAction.ADVANCEMENTS, GameMenuAction.STATISTICS,
            GameMenuAction.GIVE_FEEDBACK, GameMenuAction.REPORT_BUGS,
            GameMenuAction.OPTIONS, GameMenuAction.OPEN_TO_LAN
    };

    private static final String[] GRID_KEYS = {
            "gui.advancements", "gui.stats",
            "menu.sendFeedback", "menu.reportBugs",
            "menu.options", "menu.shareToLan"
    };

    private static final String[] GRID_FALLBACKS = {
            "Advancements", "Statistics",
            "Give Feedback", "Report Bugs",
            "Options...", "Open to LAN"
    };

    private static final String[] GRID_ICONS = {
            "★", "▮",
            "✉", "⚑",
            "⚙", "⬡"
    };

    public void render(RenderContext ctx, Theme theme, MinecraftAdapter adapter,
                       double mouseX, double mouseY, float fade, float emberPhase) {
        float eased = Easing.easeOutCubic(fade);
        GameMenuLayout layout = GameMenuLayout.compute(ctx.screenWidth(), ctx.screenHeight());

        renderWorldWash(ctx, eased);
        renderEmbers(ctx, theme, eased, emberPhase);
        renderBranding(ctx, theme, layout, eased);
        renderPanel(ctx, theme, layout, eased);
        renderPrimary(ctx, theme, adapter, layout, mouseX, mouseY, eased);
        renderSocial(ctx, theme, adapter, layout, mouseX, mouseY, eased);
        renderGrid(ctx, theme, adapter, layout, mouseX, mouseY, eased);
        renderQuit(ctx, theme, adapter, layout, mouseX, mouseY, eased);
        renderFooter(ctx, theme, layout, eased);
    }

    public GameMenuAction hitAction(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        GameMenuLayout layout = GameMenuLayout.compute(screenWidth, screenHeight);
        if (hit(mouseX, mouseY, layout.primaryX(), layout.primaryY(), layout.primaryW(), layout.primaryH())) {
            return GameMenuAction.BACK_TO_GAME;
        }
        if (hit(mouseX, mouseY, layout.socialX(), layout.socialY(), layout.socialW(), layout.socialH())) {
            return GameMenuAction.SOCIAL_HUB;
        }
        if (hit(mouseX, mouseY, layout.quitX(), layout.quitY(), layout.quitW(), layout.quitH())) {
            return GameMenuAction.SAVE_AND_QUIT;
        }
        for (int i = 0; i < GRID.length; i++) {
            int col = i % 2;
            int row = i / 2;
            if (hit(mouseX, mouseY, layout.gridCellX(col), layout.gridCellY(row),
                    layout.cellW(), layout.cellH())) {
                return GRID[i];
            }
        }
        return null;
    }

    private void renderWorldWash(RenderContext ctx, float fade) {
        int w = ctx.screenWidth();
        int h = ctx.screenHeight();
        int a = Math.round(0xA8 * fade);
        ctx.fillRect(0, 0, w, h, (a << 24) | 0x0A0A0A);
        int edge = Math.max(48, h / 5);
        int topA = Math.round(0x60 * fade) << 24;
        ctx.fillGradientVertical(0, 0, w, edge, topA, 0);
        ctx.fillGradientVertical(0, h - edge - GameMenuLayout.FOOTER_H, w, edge, 0, topA);
    }

    private void renderEmbers(RenderContext ctx, Theme theme, float fade, float phase) {
        int w = ctx.screenWidth();
        int h = ctx.screenHeight();
        int accent = theme.accent();
        for (int i = 0; i < 22; i++) {
            float seed = i * 17.13f;
            float x = ((seed * 37.1f) % 1f) * w;
            float drift = (phase * (0.12f + (i % 5) * 0.035f) + seed) % 1f;
            float y = h - drift * (h * 0.8f);
            int size = 1 + (i % 2);
            float pulse = 0.28f + 0.4f * (0.5f + 0.5f * (float) Math.sin(phase * 2.0f + seed));
            int color = ColorUtil.withAlpha(accent, fade * pulse * 0.4f);
            ctx.fillRect(Math.round(x), Math.round(y), size, size, color);
        }
    }

    private void renderBranding(RenderContext ctx, Theme theme, GameMenuLayout layout, float fade) {
        ctx.setDrawOpacity(fade);
        int centerX = ctx.screenWidth() / 2;
        PrimeLogo.drawCentered(ctx, centerX, layout.logoY(), layout.logoH(), 0xFFFFFFFF);

        float primeScale = 1.05f;
        String prime = "PRIME";
        int primeW = ctx.smoothTextWidth(prime, primeScale);
        ctx.drawSmoothText(prime, centerX - primeW / 2, layout.brandY(), theme.foreground(), primeScale);

        float clientScale = 0.72f;
        String client = "C L I E N T";
        int clientW = ctx.smoothTextWidth(client, clientScale);
        ctx.drawSmoothText(client, centerX - clientW / 2, layout.brandY() + 12,
                theme.accent(), clientScale);

        int lineW = 64;
        int lineY = layout.dividerY();
        ctx.fillGradientHorizontal(centerX - lineW, lineY, lineW - 6, 1,
                ColorUtil.withAlpha(theme.accent(), 0.05f),
                ColorUtil.withAlpha(theme.accent(), 0.75f));
        ctx.fillGradientHorizontal(centerX + 6, lineY, lineW - 6, 1,
                ColorUtil.withAlpha(theme.accent(), 0.75f),
                ColorUtil.withAlpha(theme.accent(), 0.05f));
        // Soft diamond accent
        ctx.fillRect(centerX - 2, lineY - 2, 4, 4, ColorUtil.withAlpha(theme.accent(), 0.9f));

        String title = "GAME MENU";
        float titleScale = 0.76f;
        int titleW = ctx.smoothTextWidth(title, titleScale);
        ctx.drawSmoothText(title, centerX - titleW / 2, layout.titleY(),
                ColorUtil.withAlpha(theme.accent(), 0.9f), titleScale);
        ctx.setDrawOpacity(1f);
    }

    private void renderPanel(RenderContext ctx, Theme theme, GameMenuLayout layout, float fade) {
        ctx.setDrawOpacity(fade);
        int x = layout.panelX();
        int y = layout.panelY();
        int w = layout.panelW();
        int h = layout.panelH();

        ctx.fillSoftShadow(x, y, w, h, PANEL_RADIUS, 0x78000000);
        ctx.fillRoundedBorder(x, y, w, h, PANEL_RADIUS, 1,
                ColorUtil.withAlpha(theme.accent(), 0.32f),
                ColorUtil.withAlpha(0xFF0C0C0E, 0.88f));

        // Soft top highlight — glass edge, not a tactical pinstripe
        int mid = (w - 28) / 2;
        ctx.fillGradientHorizontal(x + 14, y + 1, mid, 1,
                ColorUtil.withAlpha(theme.accent(), 0.04f),
                ColorUtil.withAlpha(theme.accent(), 0.45f));
        ctx.fillGradientHorizontal(x + 14 + mid, y + 1, w - 28 - mid, 1,
                ColorUtil.withAlpha(theme.accent(), 0.45f),
                ColorUtil.withAlpha(theme.accent(), 0.04f));
        ctx.setDrawOpacity(1f);
    }

    private void renderPrimary(RenderContext ctx, Theme theme, MinecraftAdapter adapter,
                               GameMenuLayout layout, double mouseX, double mouseY, float fade) {
        ctx.setDrawOpacity(fade);
        int x = layout.primaryX();
        int y = layout.primaryY();
        int w = layout.primaryW();
        int h = layout.primaryH();
        boolean hover = hit(mouseX, mouseY, x, y, w, h);

        int fill = hover
                ? ColorUtil.withAlpha(theme.backgroundLight(), 0.78f)
                : ColorUtil.withAlpha(0xFF161618, 0.94f);
        ctx.fillRoundedRect(x, y, w, h, BTN_RADIUS, fill);
        ctx.fillRoundedBorder(x, y, w, h, BTN_RADIUS, 1,
                ColorUtil.withAlpha(theme.accent(), hover ? 0.55f : 0.22f), fill);
        if (hover) {
            ctx.fillGradientHorizontal(x + 4, y + 1, w - 8, 1,
                    ColorUtil.withAlpha(theme.accent(), 0.15f),
                    ColorUtil.withAlpha(theme.accent(), 0.7f));
        }

        String label = adapter.translate("menu.returnToGame", "Back to Game")
                .toUpperCase(java.util.Locale.ROOT);
        float scale = 0.92f;
        int iconW = 8;
        int textW = ctx.smoothTextWidth(label, scale);
        int start = x + (w - (iconW + 6 + textW)) / 2;
        int textY = textTop(ctx, y, h, scale);
        drawPlayIcon(ctx, start, y + h / 2, theme.foreground());
        ctx.drawSmoothText(label, start + iconW + 6, textY, theme.foreground(), scale);
        ctx.setDrawOpacity(1f);
    }

    /** Full-width Social Hub — outline accent, not a solid red slab. */
    private void renderSocial(RenderContext ctx, Theme theme, MinecraftAdapter adapter,
                              GameMenuLayout layout, double mouseX, double mouseY, float fade) {
        ctx.setDrawOpacity(fade);
        int x = layout.socialX();
        int y = layout.socialY();
        int w = layout.socialW();
        int h = layout.socialH();
        boolean hover = hit(mouseX, mouseY, x, y, w, h);

        int fill = hover
                ? ColorUtil.withAlpha(theme.backgroundLight(), 0.72f)
                : ColorUtil.withAlpha(0xFF141416, 0.9f);
        ctx.fillRoundedRect(x, y, w, h, BTN_RADIUS, fill);
        ctx.fillRoundedBorder(x, y, w, h, BTN_RADIUS, 1,
                ColorUtil.withAlpha(theme.accent(), hover ? 0.75f : 0.42f), fill);

        String label = adapter.translate("prime.gui.pause.social_hub", "Social Hub")
                .toUpperCase(java.util.Locale.ROOT);
        float scale = 0.82f;
        String icon = "💬";
        int iconW = ctx.smoothTextWidth(icon, 0.85f);
        int textW = ctx.smoothTextWidth(label, scale);
        int start = x + (w - (iconW + 6 + textW)) / 2;
        int textColor = hover ? theme.foreground() : theme.foregroundMuted();
        ctx.drawSmoothText(icon, start, textTop(ctx, y, h, 0.85f), theme.accent(), 0.85f);
        ctx.drawSmoothText(label, start + iconW + 6, textTop(ctx, y, h, scale), textColor, scale);
        ctx.setDrawOpacity(1f);
    }

    private void renderGrid(RenderContext ctx, Theme theme, MinecraftAdapter adapter,
                            GameMenuLayout layout, double mouseX, double mouseY, float fade) {
        ctx.setDrawOpacity(fade);
        for (int i = 0; i < GRID.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = layout.gridCellX(col);
            int y = layout.gridCellY(row);
            int w = layout.cellW();
            int h = layout.cellH();
            boolean hover = hit(mouseX, mouseY, x, y, w, h);

            int fill = hover
                    ? ColorUtil.withAlpha(theme.backgroundLight(), 0.7f)
                    : ColorUtil.withAlpha(0xFF141416, 0.9f);
            ctx.fillRoundedRect(x, y, w, h, BTN_RADIUS, fill);
            ctx.fillRoundedBorder(x, y, w, h, BTN_RADIUS, 1,
                    ColorUtil.withAlpha(theme.accent(), hover ? 0.5f : 0.16f), fill);

            String label = adapter.translate(GRID_KEYS[i], GRID_FALLBACKS[i])
                    .toUpperCase(java.util.Locale.ROOT);
            float scale = 0.78f;
            int textColor = hover ? theme.foreground() : theme.foregroundMuted();
            ctx.drawSmoothText(GRID_ICONS[i], x + 8, textTop(ctx, y, h, 0.85f), theme.accent(), 0.85f);
            ctx.drawSmoothText(label, x + 22, textTop(ctx, y, h, scale), textColor, scale);
        }
        ctx.setDrawOpacity(1f);
    }

    private void renderQuit(RenderContext ctx, Theme theme, MinecraftAdapter adapter,
                            GameMenuLayout layout, double mouseX, double mouseY, float fade) {
        ctx.setDrawOpacity(fade);
        int x = layout.quitX();
        int y = layout.quitY();
        int w = layout.quitW();
        int h = layout.quitH();
        boolean hover = hit(mouseX, mouseY, x, y, w, h);

        int fill = hover
                ? ColorUtil.withAlpha(theme.accentSecondary(), 0.28f)
                : ColorUtil.withAlpha(0xFF121214, 0.92f);
        ctx.fillRoundedRect(x, y, w, h, BTN_RADIUS, fill);
        ctx.fillRoundedBorder(x, y, w, h, BTN_RADIUS, 1,
                ColorUtil.withAlpha(theme.accent(), hover ? 0.55f : 0.2f), fill);

        String label = adapter.translate("menu.returnToMenu", "Save and Quit to Title")
                .toUpperCase(java.util.Locale.ROOT);
        if (adapter.isMultiplayer()) {
            label = adapter.translate("menu.disconnect", "Disconnect")
                    .toUpperCase(java.util.Locale.ROOT);
        }
        float scale = 0.84f;
        int textW = ctx.smoothTextWidth(label, scale);
        int start = x + (w - textW - 14) / 2;
        ctx.drawSmoothText("⏻", start, textTop(ctx, y, h, 0.9f), theme.accent(), 0.9f);
        ctx.drawSmoothText(label, start + 14, textTop(ctx, y, h, scale), theme.foreground(), scale);
        ctx.setDrawOpacity(1f);
    }

    /** Premium text-free footer — thin bar, logo mark, geometric accent only. */
    private void renderFooter(RenderContext ctx, Theme theme, GameMenuLayout layout, float fade) {
        ctx.setDrawOpacity(fade);
        int y = layout.footerY();
        int h = layout.footerH();
        int w = ctx.screenWidth();
        int centerX = w / 2;

        ctx.fillRect(0, y, w, h, ColorUtil.withAlpha(0xFF080809, 0.9f));

        int midW = Math.min(280, w / 2);
        ctx.fillGradientHorizontal(centerX - midW, y, midW, 1,
                ColorUtil.withAlpha(theme.accent(), 0.05f),
                ColorUtil.withAlpha(theme.accent(), 0.55f));
        ctx.fillGradientHorizontal(centerX, y, midW, 1,
                ColorUtil.withAlpha(theme.accent(), 0.55f),
                ColorUtil.withAlpha(theme.accent(), 0.05f));

        int logoSize = 12;
        PrimeLogo.draw(ctx, 14, y + (h - logoSize) / 2, logoSize, 0xFFFFFFFF);

        int barW = 48;
        ctx.fillRect(centerX - barW / 2, y + h / 2, barW, 1, ColorUtil.withAlpha(theme.accent(), 0.35f));
        ctx.fillRect(centerX - 2, y + h / 2 - 2, 4, 4, ColorUtil.withAlpha(theme.accent(), 0.85f));

        ctx.setDrawOpacity(1f);
    }

    private static void drawPlayIcon(RenderContext ctx, int x, int cy, int color) {
        ctx.fillRect(x, cy - 4, 1, 8, color);
        ctx.fillRect(x + 1, cy - 3, 1, 6, color);
        ctx.fillRect(x + 2, cy - 2, 1, 4, color);
        ctx.fillRect(x + 3, cy - 1, 1, 2, color);
        ctx.fillRect(x + 4, cy, 1, 1, color);
    }

    private static int textTop(RenderContext ctx, int y, int h, float scale) {
        int glyphH = Math.max(1, Math.round(ctx.fontHeight() * scale));
        return y + (h - glyphH) / 2;
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
