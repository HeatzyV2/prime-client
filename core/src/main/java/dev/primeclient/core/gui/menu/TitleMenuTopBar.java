package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.discord.DiscordRpcService;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/** Feather-style top-right shortcuts on the title screen. */
public final class TitleMenuTopBar {

    public enum Action {
        DISCORD,
        VANILLA,
        SETTINGS,
        PROFILE
    }

    private static final int BTN = 22;
    private static final int GAP = 4;
    private static final int PAD = 10;
    private static final float ICON_SCALE = 0.88f;

    private TitleMenuTopBar() {
    }

    public static String discordUrl() {
        return "https://discord.com/application-directory/" + DiscordRpcService.APPLICATION_ID;
    }

    public static void render(RenderContext ctx, Theme theme, String playerName,
                              double mouseX, double mouseY, float fade) {
        Layout layout = layout(ctx, playerName);
        ctx.setDrawOpacity(fade);

        drawIconButton(ctx, theme, layout.discordX(), layout.y(), "D", mouseX, mouseY);
        drawIconButton(ctx, theme, layout.vanillaX(), layout.y(), "M", mouseX, mouseY);
        drawIconButton(ctx, theme, layout.settingsX(), layout.y(), "⚙", mouseX, mouseY);

        if (layout.profileW() > 0) {
            boolean hover = hit(mouseX, mouseY, layout.profileX(), layout.y(), layout.profileW(), BTN);
            int fill = hover
                    ? ColorUtil.withAlpha(theme.backgroundLight(), 0.58f)
                    : ColorUtil.withAlpha(theme.surfaceElevated(), 0.44f);
            ctx.fillRoundedRect(layout.profileX(), layout.y(), layout.profileW(), BTN,
                    PrimeDesign.RADIUS_SM, fill);
            ctx.drawSmoothText(layout.profileLabel(), layout.profileX() + 8,
                    layout.y() + (BTN - ctx.fontHeight()) / 2 + 1, theme.foreground(), 0.88f);
        }

        ctx.setDrawOpacity(1f);
    }

    public static Action hitAction(double mouseX, double mouseY, int screenWidth, String playerName) {
        Layout layout = layout(screenWidth, playerName);
        if (layout.profileW() > 0 && hit(mouseX, mouseY, layout.profileX(), layout.y(), layout.profileW(), BTN)) {
            return Action.PROFILE;
        }
        if (hit(mouseX, mouseY, layout.settingsX(), layout.y(), BTN, BTN)) {
            return Action.SETTINGS;
        }
        if (hit(mouseX, mouseY, layout.vanillaX(), layout.y(), BTN, BTN)) {
            return Action.VANILLA;
        }
        if (hit(mouseX, mouseY, layout.discordX(), layout.y(), BTN, BTN)) {
            return Action.DISCORD;
        }
        return null;
    }

    private static void drawIconButton(RenderContext ctx, Theme theme, int x, int y, String icon,
                                       double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, BTN, BTN);
        int fill = hover
                ? ColorUtil.withAlpha(theme.backgroundLight(), 0.58f)
                : ColorUtil.withAlpha(theme.surfaceElevated(), 0.44f);
        ctx.fillRoundedRect(x, y, BTN, BTN, PrimeDesign.RADIUS_SM, fill);
        if (hover) {
            ctx.fillRoundedBorder(x, y, BTN, BTN, PrimeDesign.RADIUS_SM, 1,
                    ColorUtil.withAlpha(theme.accent(), 0.55f), fill);
        }
        int iconW = ctx.smoothTextWidth(icon, ICON_SCALE);
        ctx.drawSmoothText(icon, x + (BTN - iconW) / 2, y + (BTN - ctx.fontHeight()) / 2 + 1,
                hover ? theme.foreground() : theme.foregroundMuted(), ICON_SCALE);
    }

    private static Layout layout(RenderContext ctx, String playerName) {
        String label = profileLabel(playerName);
        int profileW = label.isEmpty() ? 0 : ctx.smoothTextWidth(label, 0.88f) + 16;
        return layout(ctx.screenWidth(), profileW, label);
    }

    private static Layout layout(int screenWidth, String playerName) {
        String label = profileLabel(playerName);
        int profileW = label.isEmpty() ? 0 : label.length() * 6 + 16;
        return layout(screenWidth, profileW, label);
    }

    private static Layout layout(int screenWidth, int profileW, String label) {
        int profileX = screenWidth - PAD - profileW;
        int settingsX = profileX - (profileW > 0 ? GAP : 0) - BTN;
        int vanillaX = settingsX - GAP - BTN;
        int discordX = vanillaX - GAP - BTN;
        return new Layout(discordX, vanillaX, settingsX, profileX, profileW, PAD, label);
    }

    private static String profileLabel(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "";
        }
        String trimmed = playerName.trim();
        if (trimmed.length() > 12) {
            return trimmed.substring(0, 11) + "…";
        }
        return trimmed;
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private record Layout(int discordX, int vanillaX, int settingsX, int profileX, int profileW,
                          int y, String profileLabel) {
    }
}
