package dev.primeclient.core.hud.elements;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.util.ColorUtil;

/** Directional sound radar compass widget on the HUD. */
public final class SoundRadarElement extends HudElement {

    private final ThemeManager themes;
    private long lastSoundTime = 0L;
    private float soundAngle = 0f;

    public SoundRadarElement(ThemeManager themes) {
        super("sound-radar", "Sound Radar", HudAnchor.CENTER, 0, 35);
        this.themes = themes;
    }

    public void triggerSound(float angleDegrees) {
        this.soundAngle = angleDegrees;
        this.lastSoundTime = System.currentTimeMillis();
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return 40;
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        return 40;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        long elapsed = nowMillis - lastSoundTime;
        if (elapsed < 0 || elapsed > 800L) {
            return;
        }

        float alpha = 1f - (elapsed / 800f);
        Theme theme = themes.active();
        int color = ColorUtil.withAlpha(theme.accent(), alpha * 0.85f);

        int cx = measureWidth(ctx) / 2;
        int cy = measureHeight(ctx) / 2;
        int radius = 14;

        // Outer ring
        ctx.fillRoundedBorder(cx - radius, cy - radius, radius * 2, radius * 2, radius, 1, color, 0x00000000);

        // Sound directional pulse dot
        double rad = Math.toRadians(soundAngle);
        int px = cx + (int) Math.round(Math.cos(rad) * (radius - 2));
        int py = cy + (int) Math.round(Math.sin(rad) * (radius - 2));
        ctx.fillRect(px - 1, py - 1, 3, 3, color);
    }
}
