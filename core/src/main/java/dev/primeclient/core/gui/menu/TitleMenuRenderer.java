package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.design.PrimeLogo;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.Easing;

/**
 * Full-screen title menu — Feather-inspired layout with gradient backdrop,
 * branding block, and pill navigation buttons.
 */
public final class TitleMenuRenderer {

    private static final int BUTTON_W = 220;
    private static final int BUTTON_H = 30;
    private static final int BUTTON_GAP = 8;
    private static final int SIDE_PAD = 48;
    private static final TitleMenuAction[] ACTIONS = TitleMenuAction.values();
    private static final String[] LABELS = {
            "Singleplayer",
            "Multiplayer",
            "Prime Client",
            "Options",
            "Quit Game"
    };

    private float particlePhase;

    public void tick(float deltaSeconds) {
        particlePhase += deltaSeconds * 0.55f;
    }

    public void render(RenderContext ctx, Theme theme, double mouseX, double mouseY,
                       String minecraftVersion, String clientVersion, float fade) {
        float eased = Easing.easeOutCubic(fade);
        renderBackground(ctx, theme, eased);
        renderBranding(ctx, theme, clientVersion, eased);
        renderButtons(ctx, theme, mouseX, mouseY, eased);
        renderFooter(ctx, theme, minecraftVersion, clientVersion, eased);
    }

    private void renderBackground(RenderContext ctx, Theme theme, float fade) {
        // Panorama is drawn by PrimeTitleScreen — keep a soft dark overlay for UI contrast.
        int overlayAlpha = Math.round(150 * fade);
        ctx.fillRect(0, 0, ctx.screenWidth(), ctx.screenHeight(), (overlayAlpha << 24));

        for (int i = 0; i < 18; i++) {
            float px = (float) ((Math.sin(particlePhase + i * 1.4) + 1) * 0.5 * ctx.screenWidth());
            float py = (float) ((Math.cos(particlePhase * 0.75 + i * 2.3) + 1) * 0.5 * ctx.screenHeight());
            int size = 1 + (i % 3);
            int alpha = 0x18 + (i % 4) * 0x10;
            ctx.fillRect(Math.round(px), Math.round(py), size, size, (alpha << 24) | (theme.accent() & 0x00FFFFFF));
        }

        int accentLineW = Math.round(ctx.screenWidth() * 0.42f * fade);
        ctx.fillRect(SIDE_PAD, ctx.screenHeight() / 2 + 60, accentLineW, 1,
                withAlpha(theme.accent(), 0.35f * fade));
    }

    private void renderBranding(RenderContext ctx, Theme theme, String clientVersion, float fade) {
        int leftX = SIDE_PAD;
        int centerY = ctx.screenHeight() / 2;

        ctx.setDrawOpacity(fade);
        int logoH = Math.min(72, Math.max(48, ctx.screenHeight() / 10));
        PrimeLogo.draw(ctx, leftX, centerY - logoH - 56, logoH, 0xFFFFFFFF);

        ctx.drawSmoothText("Prime Client", leftX, centerY - 36, theme.foreground(), 1.35f);
        ctx.drawSmoothText(PrimeDesign.TAGLINE, leftX, centerY - 14, theme.foregroundMuted(), 0.95f);
        ctx.drawSmoothText("v" + clientVersion, leftX, centerY + 6, theme.accent(), 0.9f);
        ctx.setDrawOpacity(1f);
    }

    private void renderButtons(RenderContext ctx, Theme theme, double mouseX, double mouseY, float fade) {
        int stackH = ACTIONS.length * BUTTON_H + (ACTIONS.length - 1) * BUTTON_GAP;
        int startY = (ctx.screenHeight() - stackH) / 2;
        int x = ctx.screenWidth() - SIDE_PAD - BUTTON_W;

        ctx.setDrawOpacity(fade);
        for (int i = 0; i < ACTIONS.length; i++) {
            int y = startY + i * (BUTTON_H + BUTTON_GAP);
            drawPillButton(ctx, theme, LABELS[i], x, y, BUTTON_W, mouseX, mouseY, i == 2);
        }
        ctx.setDrawOpacity(1f);
    }

    private void drawPillButton(RenderContext ctx, Theme theme, String label,
                                int x, int y, int w, double mouseX, double mouseY, boolean accentStyle) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + BUTTON_H;
        int fill = hover
                ? (accentStyle ? blend(theme.accent(), theme.backgroundLight(), 0.35f) : theme.backgroundLight())
                : (accentStyle ? blend(theme.accent(), theme.surfaceElevated(), 0.18f) : theme.surfaceElevated());
        ctx.fillRect(x, y, w, BUTTON_H, fill);

        if (hover) {
            ctx.fillRect(x, y, 2, BUTTON_H, theme.accent());
            ctx.fillRect(x, y + BUTTON_H - 1, w, 1, withAlpha(theme.accent(), 0.55f));
        } else if (accentStyle) {
            ctx.fillRect(x, y, 2, BUTTON_H, withAlpha(theme.accent(), 0.75f));
        }

        int textColor = hover ? theme.accent() : (accentStyle ? theme.foreground() : theme.foregroundMuted());
        int textW = ctx.smoothTextWidth(label, 1f);
        ctx.drawSmoothText(label, x + (w - textW) / 2, y + (BUTTON_H - ctx.fontHeight()) / 2 + 1, textColor, 1f);
    }

    private void renderFooter(RenderContext ctx, Theme theme, String minecraftVersion,
                              String clientVersion, float fade) {
        ctx.setDrawOpacity(fade * 0.85f);
        String left = "Minecraft " + minecraftVersion;
        String right = PrimeDesign.VERSION.equals(clientVersion)
                ? "Prime Client"
                : "Prime Client v" + clientVersion;
        ctx.drawSmoothText(left, SIDE_PAD, ctx.screenHeight() - 22, theme.foregroundMuted(), 0.85f);
        int rightW = ctx.smoothTextWidth(right, 0.85f);
        ctx.drawSmoothText(right, ctx.screenWidth() - SIDE_PAD - rightW, ctx.screenHeight() - 22,
                theme.foregroundMuted(), 0.85f);
        ctx.setDrawOpacity(1f);
    }

    public TitleMenuAction hitAction(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        int stackH = ACTIONS.length * BUTTON_H + (ACTIONS.length - 1) * BUTTON_GAP;
        int startY = (screenHeight - stackH) / 2;
        int x = screenWidth - SIDE_PAD - BUTTON_W;

        for (int i = 0; i < ACTIONS.length; i++) {
            int y = startY + i * (BUTTON_H + BUTTON_GAP);
            if (mouseX >= x && mouseX < x + BUTTON_W && mouseY >= y && mouseY < y + BUTTON_H) {
                return ACTIONS[i];
            }
        }
        return null;
    }

    private static int withAlpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24) & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >>> 24) & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        int al = Math.round(aa + (ba - aa) * t);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }
}
