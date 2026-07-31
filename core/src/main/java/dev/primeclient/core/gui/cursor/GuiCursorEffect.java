package dev.primeclient.core.gui.cursor;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/**
 * Premium cursor trail particle system and neon indicator.
 * Completely allocation-free in render loop using fixed primitive arrays.
 */
public final class GuiCursorEffect {

    private static final int MAX_PARTICLES = 32;

    private static final float[] PX = new float[MAX_PARTICLES];
    private static final float[] PY = new float[MAX_PARTICLES];
    private static final float[] VX = new float[MAX_PARTICLES];
    private static final float[] VY = new float[MAX_PARTICLES];
    private static final float[] LIFE = new float[MAX_PARTICLES];
    private static final float[] MAX_LIFE = new float[MAX_PARTICLES];

    private static int nextIndex = 0;
    private static double lastX = -1;
    private static double lastY = -1;

    private GuiCursorEffect() {
    }

    public static void updateAndRender(RenderContext ctx, Theme theme, double mouseX, double mouseY, float deltaSeconds) {
        if (lastX >= 0 && lastY >= 0) {
            double dx = mouseX - lastX;
            double dy = mouseY - lastY;
            double distSq = dx * dx + dy * dy;

            // Spawn particles when mouse moves
            if (distSq > 4.0) {
                spawnParticle((float) mouseX, (float) mouseY, (float) (-dx * 0.15f), (float) (-dy * 0.15f));
            }
        }
        lastX = mouseX;
        lastY = mouseY;

        // Update and render particles
        int accent = theme.accent();
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (LIFE[i] <= 0f) {
                continue;
            }
            LIFE[i] -= deltaSeconds;
            if (LIFE[i] <= 0f) {
                continue;
            }

            PX[i] += VX[i] * deltaSeconds * 60f;
            PY[i] += VY[i] * deltaSeconds * 60f;

            float alpha = Math.max(0f, LIFE[i] / MAX_LIFE[i]);
            int color = ColorUtil.withAlpha(accent, alpha * 0.7f);
            int size = (alpha > 0.5f) ? 2 : 1;

            ctx.fillRect(Math.round(PX[i]), Math.round(PY[i]), size, size, color);
        }

        // Draw sleek glowing dot indicator at current mouse cursor
        int cursorColor = ColorUtil.withAlpha(accent, 0.85f);
        int mx = (int) Math.round(mouseX);
        int my = (int) Math.round(mouseY);
        ctx.fillRect(mx - 1, my - 1, 3, 3, cursorColor);
        ctx.fillRect(mx, my, 1, 1, 0xFFFFFFFF);
    }

    private static void spawnParticle(float x, float y, float vx, float vy) {
        int idx = nextIndex;
        nextIndex = (nextIndex + 1) % MAX_PARTICLES;

        PX[idx] = x + (float) ((Math.random() - 0.5) * 4.0);
        PY[idx] = y + (float) ((Math.random() - 0.5) * 4.0);
        VX[idx] = vx + (float) ((Math.random() - 0.5) * 0.5);
        VY[idx] = vy + (float) ((Math.random() - 0.5) * 0.5);
        LIFE[idx] = 0.25f + (float) (Math.random() * 0.15);
        MAX_LIFE[idx] = LIFE[idx];
    }
}
