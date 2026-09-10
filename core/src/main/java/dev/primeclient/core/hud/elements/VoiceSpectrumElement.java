package dev.primeclient.core.hud.elements;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.util.ColorUtil;

/** Voice activity audio spectrum HUD overlay. */
public final class VoiceSpectrumElement extends HudElement {

    private final ThemeManager themes;
    private boolean voiceActive = false;

    public VoiceSpectrumElement(ThemeManager themes) {
        super("voice-spectrum", "Voice Spectrum", HudAnchor.TOP_LEFT, 10, 30);
        this.themes = themes;
    }

    public void setVoiceActive(boolean active) {
        this.voiceActive = active;
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return 72;
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        return 22;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        Theme theme = themes.active();
        int w = measureWidth(ctx);
        int h = measureHeight(ctx);
        int radius = PrimeDesign.RADIUS_SM;

        int fill = ColorUtil.withAlpha(0xFF0C0C0E, 0.9f);
        ctx.fillSoftShadow(0, 0, w, h, radius, 0x70000000);
        ctx.fillRoundedBorder(0, 0, w, h, radius, 1, ColorUtil.withAlpha(theme.accent(), 0.5f), fill);

        // Icon
        int iconColor = voiceActive ? theme.accent() : theme.foregroundMuted();
        ctx.drawSmoothText("🎤", 6, (h - ctx.fontHeight()) / 2 + 1, iconColor, 0.78f);

        // 5 spectrum bars
        int barX = 26;
        int barW = 3;
        int gap = 3;
        int maxBarH = 12;
        int baseY = h - 5;

        double time = nowMillis / 150.0;
        for (int i = 0; i < 5; i++) {
            float heightFactor = voiceActive
                    ? (float) (0.35f + 0.65f * Math.abs(Math.sin(time + i * 0.85)))
                    : 0.2f;
            int barH = Math.max(2, Math.round(maxBarH * heightFactor));
            int col = voiceActive ? theme.accent() : theme.foregroundMuted();
            ctx.fillRect(barX + i * (barW + gap), baseY - barH, barW, barH, ColorUtil.withAlpha(col, 0.85f));
        }
    }
}
