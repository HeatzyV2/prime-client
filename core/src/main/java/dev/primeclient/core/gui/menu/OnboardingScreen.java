package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.design.PrimeLogo;
import dev.primeclient.core.theme.Theme;

/**
 * Interactive first-run wizard inside the ClickGUI.
 *
 * <p>Steps: theme → profile preset → keybinds → finish.</p>
 */
public final class OnboardingScreen {

    public static final int PANEL_W = 300;
    public static final int PANEL_H = 168;

    private OnboardingScreen() {
    }

    public static void render(RenderContext ctx, Theme theme, OnboardingManager onboarding,
                              int screenW, int screenH, float menuSlide, double mouseX, double mouseY) {
        int x = (screenW - PANEL_W) / 2;
        int y = (screenH - PANEL_H) / 2 + Math.round(menuSlide);
        ctx.fillRect(x, y, PANEL_W, PANEL_H, theme.background());
        ctx.fillRect(x, y, PANEL_W, 2, theme.accent());
        PrimeLogo.drawCentered(ctx, x + PANEL_W / 2, y + 10, 18, 0xFFFFFFFF);

        int step = onboarding.step();
        switch (step) {
            case 0 -> renderThemeStep(ctx, theme, onboarding, x, y, mouseX, mouseY);
            case 1 -> renderProfileStep(ctx, theme, onboarding, x, y, mouseX, mouseY);
            case 2 -> renderKeybindStep(ctx, theme, x, y);
            default -> renderFinishStep(ctx, theme, x, y);
        }

        ctx.drawText("Étape " + Math.min(step + 1, 4) + "/4  •  Échap = passer",
                x + 12, y + PANEL_H - 14, theme.foregroundMuted(), true);
    }

    public static boolean mousePressed(OnboardingManager onboarding, double mx, double my,
                                       int screenW, int screenH, float menuSlide, int button) {
        if (button != 0) {
            return false;
        }
        int x = (screenW - PANEL_W) / 2;
        int y = (screenH - PANEL_H) / 2 + Math.round(menuSlide);
        if (mx < x || mx >= x + PANEL_W || my < y || my >= y + PANEL_H) {
            return false;
        }
        return switch (onboarding.step()) {
            case 0 -> handleThemeClick(onboarding, mx, my, x, y);
            case 1 -> handleProfileClick(onboarding, mx, my, x, y);
            case 2, 3 -> {
                onboarding.nextStep();
                yield true;
            }
            default -> {
                onboarding.nextStep();
                yield true;
            }
        };
    }

    private static void renderThemeStep(RenderContext ctx, Theme theme, OnboardingManager onboarding,
                                        int x, int y, double mouseX, double mouseY) {
        ctx.drawText("Choisis ton thème", x + 12, y + 36, theme.accent(), true);
        drawChoice(ctx, theme, x + 12, y + 56, 128, "Prime Dark",
                "prime-dark".equals(onboarding.chosenTheme()), mouseX, mouseY);
        drawChoice(ctx, theme, x + 148, y + 56, 128, "Prime Light",
                "prime-light".equals(onboarding.chosenTheme()), mouseX, mouseY);
        ctx.drawText("Clique une option puis continue", x + 12, y + 88, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 108, PANEL_W - 24, "Continuer", mouseX, mouseY);
    }

    private static void renderProfileStep(RenderContext ctx, Theme theme, OnboardingManager onboarding,
                                          int x, int y, double mouseX, double mouseY) {
        ctx.drawText("Profil de modules", x + 12, y + 36, theme.accent(), true);
        drawChoice(ctx, theme, x + 12, y + 56, 88, "Équilibré",
                "default".equals(onboarding.chosenProfile()), mouseX, mouseY);
        drawChoice(ctx, theme, x + 106, y + 56, 88, "PvP",
                "pvp".equals(onboarding.chosenProfile()), mouseX, mouseY);
        drawChoice(ctx, theme, x + 200, y + 56, 88, "Survie",
                "survival".equals(onboarding.chosenProfile()), mouseX, mouseY);
        ctx.drawText("FPS, coords, crosshair, Discord RPC inclus", x + 12, y + 88, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 108, PANEL_W - 24, "Continuer", mouseX, mouseY);
    }

    private static void renderKeybindStep(RenderContext ctx, Theme theme, int x, int y) {
        ctx.drawText("Raccourcis essentiels", x + 12, y + 36, theme.accent(), true);
        ctx.drawText("Right Shift  →  Menu Prime (modules, réglages)", x + 12, y + 56, theme.foreground(), true);
        ctx.drawText("H  →  HUD Editor (déplacer les éléments)", x + 12, y + 72, theme.foreground(), true);
        ctx.drawText("C  →  Zoom (module Zoom, maintenir)", x + 12, y + 88, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 108, PANEL_W - 24, "Compris !", 0, 0);
    }

    private static void renderFinishStep(RenderContext ctx, Theme theme, int x, int y) {
        ctx.drawText("C'est parti !", x + 12, y + 36, theme.accent(), true);
        ctx.drawText("Ton HUD, crosshair et Discord RPC sont prêts.", x + 12, y + 56, theme.foreground(), true);
        ctx.drawText("Explore Modules dans le menu pour tout personnaliser.", x + 12, y + 72, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 108, PANEL_W - 24, "Entrer dans Prime Client", 0, 0);
    }

    private static boolean handleThemeClick(OnboardingManager onboarding, double mx, double my, int x, int y) {
        if (hit(mx, my, x + 12, y + 56, 128, 22)) {
            onboarding.setChosenTheme("prime-dark");
            return true;
        }
        if (hit(mx, my, x + 148, y + 56, 128, 22)) {
            onboarding.setChosenTheme("prime-light");
            return true;
        }
        if (hit(mx, my, x + 12, y + 108, PANEL_W - 24, 22)) {
            onboarding.nextStep();
            return true;
        }
        return true;
    }

    private static boolean handleProfileClick(OnboardingManager onboarding, double mx, double my, int x, int y) {
        if (hit(mx, my, x + 12, y + 56, 88, 22)) {
            onboarding.setChosenProfile("default");
            return true;
        }
        if (hit(mx, my, x + 106, y + 56, 88, 22)) {
            onboarding.setChosenProfile("pvp");
            return true;
        }
        if (hit(mx, my, x + 200, y + 56, 88, 22)) {
            onboarding.setChosenProfile("survival");
            return true;
        }
        if (hit(mx, my, x + 12, y + 108, PANEL_W - 24, 22)) {
            onboarding.nextStep();
            return true;
        }
        return true;
    }

    private static void drawChoice(RenderContext ctx, Theme theme, int x, int y, int w, String label,
                                   boolean selected, double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, w, 22);
        ctx.fillRect(x, y, w, 22, selected ? theme.surfaceElevated() : theme.backgroundLight());
        if (selected || hover) {
            ctx.fillRect(x, y + 21, w, 1, theme.accent());
        }
        ctx.drawText(label, x + 6, y + 7, selected ? theme.accent() : theme.foreground(), true);
    }

    private static void drawPrimary(RenderContext ctx, Theme theme, int x, int y, int w, String label,
                                    double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, w, 22);
        ctx.fillRect(x, y, w, 22, hover ? theme.accent() : theme.backgroundLight());
        int textColor = hover ? theme.background() : theme.foreground();
        ctx.drawText(label, x + (w - ctx.textWidth(label)) / 2, y + 7, textColor, true);
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
