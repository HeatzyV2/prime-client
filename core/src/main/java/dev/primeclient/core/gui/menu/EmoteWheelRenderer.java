package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.cosmetics.EmoteCatalog;
import dev.primeclient.core.cosmetics.EmoteDefinition;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.state.EmoteState;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/** Radial emote wheel opened via {@code /emotes}. */
public final class EmoteWheelRenderer {

    private static final int RADIUS = 90;

    public void render(RenderContext ctx, Theme theme, int screenW, int screenH, double mouseX, double mouseY) {
        if (!EmoteState.wheelOpen()) {
            return;
        }
        List<EmoteDefinition> emotes = new ArrayList<>(EmoteCatalog.all().values());
        int cx = screenW / 2;
        int cy = screenH / 2;
        ctx.fillRoundedRect(cx - RADIUS - 20, cy - RADIUS - 20, (RADIUS + 20) * 2, (RADIUS + 20) * 2,
                PrimeDesign.RADIUS_LG, ColorUtil.withAlpha(theme.background(), 0.72f));
        GuiLayout.label(ctx, "Emotes", cx - 18, cy - RADIUS - 12, theme.accent());

        int n = emotes.size();
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + (i * Math.PI * 2 / n);
            int ex = cx + (int) (Math.cos(angle) * RADIUS);
            int ey = cy + (int) (Math.sin(angle) * RADIUS);
            boolean hover = dist(mouseX, mouseY, ex, ey) < 28;
            ctx.fillRoundedRect(ex - 26, ey - 12, 52, 24, PrimeDesign.RADIUS_SM,
                    hover ? theme.surfaceElevated() : theme.backgroundLight());
            GuiLayout.label(ctx, emotes.get(i).name(), ex - 20, ey - 4,
                    hover ? theme.accent() : theme.foreground());
        }
        GuiLayout.label(ctx, "Click · Esc /emotes close", cx - 55, cy + RADIUS + 8, theme.foregroundMuted());
    }

    public boolean mousePressed(double mx, double my, int screenW, int screenH) {
        if (!EmoteState.wheelOpen()) {
            return false;
        }
        List<EmoteDefinition> emotes = new ArrayList<>(EmoteCatalog.all().values());
        int cx = screenW / 2;
        int cy = screenH / 2;
        int n = emotes.size();
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + (i * Math.PI * 2 / n);
            int ex = cx + (int) (Math.cos(angle) * RADIUS);
            int ey = cy + (int) (Math.sin(angle) * RADIUS);
            if (dist(mx, my, ex, ey) < 28) {
                EmoteState.playLocal(emotes.get(i).id());
                EmoteState.setWheelOpen(false);
                return true;
            }
        }
        return true; // consume clicks while open
    }

    private static double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
