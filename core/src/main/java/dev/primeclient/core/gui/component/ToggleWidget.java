package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.Easing;

/** Animated on/off toggle switch. */
public final class ToggleWidget {

    private float knobProgress = 0f;

    public void tick(boolean on, float deltaSeconds) {
        knobProgress = Easing.lerp(knobProgress, on ? 1f : 0f, deltaSeconds * PrimeDesign.MOTION_FAST);
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, boolean on) {
        int w = PrimeDesign.TOGGLE_WIDTH;
        int h = PrimeDesign.TOGGLE_HEIGHT;
        int track = on ? theme.accent() : theme.backgroundLight();
        ctx.fillRect(x, y, w, h, track);
        int knob = 7;
        int knobX = x + 2 + Math.round((w - knob - 4) * knobProgress);
        int knobY = y + (h - knob) / 2;
        ctx.fillRect(knobX, knobY, knob, knob, theme.foreground());
    }

    public boolean hit(double mx, double my, int x, int y) {
        return mx >= x && mx < x + PrimeDesign.TOGGLE_WIDTH
                && my >= y && my < y + PrimeDesign.TOGGLE_HEIGHT;
    }
}
