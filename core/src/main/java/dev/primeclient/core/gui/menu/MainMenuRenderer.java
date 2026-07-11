package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.design.PrimeLogo;
import dev.primeclient.core.gui.clickgui.ClickGuiView;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.Easing;

/**
 * Premium main menu renderer — gradient backdrop, logo block, player profile, nav buttons.
 */
public final class MainMenuRenderer {

    public static final int PANEL_WIDTH = 220;
    private static final int LOGO_HEIGHT = 36;
    private static final int BUTTON_H = PrimeDesign.MENU_BUTTON_HEIGHT;

    private float particlePhase;

    public void tick(float deltaSeconds) {
        particlePhase += deltaSeconds * 0.6f;
    }

    public void renderBackground(RenderContext ctx, Theme theme, float openFade) {
        ctx.fillGradientVertical(0, 0, ctx.screenWidth(), ctx.screenHeight(),
                theme.gradientTop(), theme.gradientBottom());
        int overlayAlpha = Math.round(120 * Easing.easeOutCubic(openFade));
        ctx.fillRect(0, 0, ctx.screenWidth(), ctx.screenHeight(), (overlayAlpha << 24));

        // Light particle accents
        for (int i = 0; i < 12; i++) {
            float px = (float) ((Math.sin(particlePhase + i * 1.7) + 1) * 0.5 * ctx.screenWidth());
            float py = (float) ((Math.cos(particlePhase * 0.8 + i * 2.1) + 1) * 0.5 * ctx.screenHeight());
            int size = 2 + (i % 2);
            ctx.fillRect(Math.round(px), Math.round(py), size, size, theme.accent() & 0x40FFFFFF);
        }
    }

    public int panelX(int screenWidth) {
        return (screenWidth - PANEL_WIDTH) / 2;
    }

    public int panelY(int screenHeight, float menuSlide) {
        return (screenHeight - panelHeight()) / 2 + Math.round(menuSlide);
    }

    public int panelHeight() {
        return PrimeDesign.SPACE_2XL + LOGO_HEIGHT + PrimeDesign.SPACE_LG
                + 6 * (BUTTON_H + PrimeDesign.SPACE_SM) + PrimeDesign.SPACE_XL + 8;
    }

    public void renderPanel(RenderContext ctx, Theme theme, int x, int y,
                          String playerName, String version, double mouseX, double mouseY) {
        ctx.fillRect(x, y, PANEL_WIDTH, panelHeight(), theme.background());
        ctx.fillRect(x, y, PANEL_WIDTH, 2, theme.accent());

        // Logo
        int logoH = 22;
        PrimeLogo.drawCentered(ctx, x + PANEL_WIDTH / 2, y + PrimeDesign.SPACE_LG, logoH, 0xFFFFFFFF);
        ctx.drawText("v" + version, x + (PANEL_WIDTH - ctx.textWidth("v" + version)) / 2,
                y + PrimeDesign.SPACE_LG + logoH + 2, theme.foregroundMuted(), true);

        // Profile
        String profile = playerName == null || playerName.isBlank() ? "Guest" : playerName;
        ctx.fillRect(x + PrimeDesign.SPACE_LG, y + LOGO_HEIGHT + PrimeDesign.SPACE_MD,
                PANEL_WIDTH - PrimeDesign.SPACE_LG * 2, 18, theme.surfaceElevated());
        ctx.drawText(profile, x + PrimeDesign.SPACE_LG + 6, y + LOGO_HEIGHT + PrimeDesign.SPACE_MD + 5,
                theme.foreground(), true);

        int rowY = y + LOGO_HEIGHT + PrimeDesign.SPACE_2XL;
        int rowW = PANEL_WIDTH - PrimeDesign.SPACE_LG * 2;
        int rowX = x + PrimeDesign.SPACE_LG;
        drawButton(ctx, theme, "Play", rowX, rowY, rowW, mouseX, mouseY, 0);
        rowY += BUTTON_H + PrimeDesign.SPACE_SM;
        drawButton(ctx, theme, "Modules", rowX, rowY, rowW, mouseX, mouseY, 1);
        rowY += BUTTON_H + PrimeDesign.SPACE_SM;
        drawButton(ctx, theme, "HUD Editor", rowX, rowY, rowW, mouseX, mouseY, 2);
        rowY += BUTTON_H + PrimeDesign.SPACE_SM;
        drawButton(ctx, theme, "Configurations", rowX, rowY, rowW, mouseX, mouseY, 3);
        rowY += BUTTON_H + PrimeDesign.SPACE_SM;
        drawButton(ctx, theme, "Cosmetics", rowX, rowY, rowW, mouseX, mouseY, 4);
        rowY += BUTTON_H + PrimeDesign.SPACE_SM;
        drawButton(ctx, theme, "Settings", rowX, rowY, rowW, mouseX, mouseY, 5);
        rowY += BUTTON_H + PrimeDesign.SPACE_SM;

        ctx.drawText("Right Shift = menu  •  H = HUD Editor", x + PrimeDesign.SPACE_LG, y + panelHeight() - 14,
                theme.foregroundMuted(), true);
    }

    private void drawButton(RenderContext ctx, Theme theme, String label, int x, int y, int w,
                            double mouseX, double mouseY, int index) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + BUTTON_H;
        ctx.fillRect(x, y, w, BUTTON_H, hover ? theme.backgroundLight() : theme.surfaceElevated());
        if (hover) {
            ctx.fillRect(x, y + BUTTON_H - 1, w, 1, theme.accent());
        }
        ctx.drawText(label, x + PrimeDesign.SPACE_MD, y + (BUTTON_H - ctx.fontHeight()) / 2 + 1,
                hover ? theme.accent() : theme.foreground(), true);
    }

    public ClickGuiView viewForButton(int index) {
        return switch (index) {
            case 0 -> null; // Play = close
            case 1 -> ClickGuiView.BROWSE;
            case 2 -> null; // HUD editor handled separately
            case 3 -> ClickGuiView.CONFIGURATIONS;
            case 4 -> ClickGuiView.COSMETICS;
            case 5 -> ClickGuiView.SETTINGS;
            default -> ClickGuiView.MAIN_MENU;
        };
    }

    public int hitButton(double mouseX, double mouseY, int x, int y) {
        int rowY = y + LOGO_HEIGHT + PrimeDesign.SPACE_2XL;
        int rowW = PANEL_WIDTH - PrimeDesign.SPACE_LG * 2;
        int rowX = x + PrimeDesign.SPACE_LG;
        for (int i = 0; i < 6; i++) {
            if (mouseX >= rowX && mouseX < rowX + rowW && mouseY >= rowY && mouseY < rowY + BUTTON_H) {
                return i;
            }
            rowY += BUTTON_H + PrimeDesign.SPACE_SM;
        }
        return -1;
    }
}
