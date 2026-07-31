package dev.primeclient.core.hud.elements;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.util.ColorUtil;

/** Neon X-hitmarker rendered at screen center upon landing hits. */
public final class HitMarkerElement extends HudElement {

    private static final long DURATION_MS = 250L;
    private final ThemeManager themes;
    private long lastHitTime = 0L;

    public HitMarkerElement(ThemeManager themes) {
        super("hit-marker", "Hit Marker", HudAnchor.CENTER, 0, 0);
        this.themes = themes;
    }

    public void triggerHit() {
        this.lastHitTime = System.currentTimeMillis();
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return 16;
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        return 16;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        long elapsed = nowMillis - lastHitTime;
        if (elapsed < 0 || elapsed > DURATION_MS) {
            return;
        }

        float alpha = 1f - (elapsed / (float) DURATION_MS);
        Theme theme = themes.active();
        int color = ColorUtil.withAlpha(theme.accent(), alpha * 0.9f);

        int cx = ctx.screenWidth() / 2;
        int cy = ctx.screenHeight() / 2;
        int gap = 3;
        int len = 5;

        // Top-Left diagonal
        ctx.fillRect(cx - gap - len, cy - gap - len, len, 1, color);
        ctx.fillRect(cx - gap - len, cy - gap - len, 1, len, color);

        // Top-Right diagonal
        ctx.fillRect(cx + gap + 1, cy - gap - len, len, 1, color);
        ctx.fillRect(cx + gap + len, cy - gap - len, 1, len, color);

        // Bottom-Left diagonal
        ctx.fillRect(cx - gap - len, cy + gap + 1, len, 1, color);
        ctx.fillRect(cx - gap - len, cy + gap + 1, 1, len, color);

        // Bottom-Right diagonal
        ctx.fillRect(cx + gap + 1, cy + gap + len, len, 1, color);
        ctx.fillRect(cx + gap + len, cy + gap + 1, 1, len, color);
    }
}
