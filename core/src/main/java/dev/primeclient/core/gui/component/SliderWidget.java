package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;

/** Horizontal slider with visible value label — track sizes from {@link PrimeDesign}. */
public final class SliderWidget {

    /** Full-height card/settings layout (label + value + track below). */
    public void render(RenderContext ctx, Theme theme, int x, int y, int width,
                       String label, String valueText, float fraction, boolean hovered) {
        ctx.drawText(label, x, y, theme.foreground(), true);
        if (valueText != null && !valueText.isEmpty()) {
            ctx.drawText(valueText, x + width - ctx.textWidth(valueText), y, theme.foregroundMuted(), true);
        }
        int barY = y + ctx.fontHeight() + 2;
        int barH = PrimeDesign.SLIDER_TRACK_H;
        ctx.fillRect(x, barY, width, barH, theme.backgroundLight());
        int fill = Math.round(width * Math.clamp(fraction, 0f, 1f));
        ctx.fillRect(x, barY, fill, barH, hovered ? theme.accentSecondary() : theme.accent());
    }

    /** Compact ClickGUI panel row: label + value on the row, track under the text. */
    public void renderCompact(RenderContext ctx, Theme theme, int labelX, int textY,
                              String name, String value, int valueX,
                              int barX, int barY, int barWidth, float fraction) {
        ctx.drawUiText(value, valueX, textY, theme.foregroundMuted());
        ctx.drawUiText(name, labelX, textY, theme.foreground());
        int barH = PrimeDesign.SLIDER_TRACK_H_COMPACT;
        ctx.fillRect(barX, barY, barWidth, barH, theme.backgroundLight());
        ctx.fillRect(barX, barY, Math.round(barWidth * Math.clamp(fraction, 0f, 1f)), barH, theme.accent());
    }

    public float fractionFromMouse(double mouseX, int x, int width) {
        return Math.clamp((float) (mouseX - x) / width, 0f, 1f);
    }
}
