package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/** In-game radial quick wheel menu for profile switching and shortcuts. */
public final class RadialMenuRenderer {

    private static final String[] SLICE_LABELS = {
            "PVP Profile", "SMP Profile", "Social Hub", "ClickGUI", "Stream Mode", "Settings"
    };

    private static final String[] SLICE_ICONS = {
            "⚔", "🌾", "💬", "⚙", "🎥", "✦"
    };

    private RadialMenuRenderer() {
    }

    public static void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        int cx = ctx.screenWidth() / 2;
        int cy = ctx.screenHeight() / 2;
        int radius = 90;
        int innerRadius = 32;

        // Dark wash backdrop
        ctx.fillRect(0, 0, ctx.screenWidth(), ctx.screenHeight(), 0x90000000);

        // Center hub
        ctx.fillSoftShadow(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2, innerRadius, 0x80000000);
        ctx.fillRoundedBorder(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2, innerRadius, 1,
                ColorUtil.withAlpha(theme.accent(), 0.85f), ColorUtil.withAlpha(0xFF0C0C0E, 0.95f));
        ctx.drawSmoothText("PRIME", cx - ctx.smoothTextWidth("PRIME", 0.72f) / 2, cy - 3, theme.accent(), 0.72f);

        // Render 6 radial slices
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;

        int count = SLICE_LABELS.length;
        double sliceAngle = 360.0 / count;

        for (int i = 0; i < count; i++) {
            double startAngle = i * sliceAngle;
            double endAngle = (i + 1) * sliceAngle;
            boolean hover = dist >= innerRadius && dist <= radius + 15 && angle >= startAngle && angle < endAngle;

            double midAngle = Math.toRadians(startAngle + sliceAngle / 2);
            int itemDist = 62;
            int ix = cx + (int) Math.round(Math.cos(midAngle) * itemDist);
            int iy = cy + (int) Math.round(Math.sin(midAngle) * itemDist);

            int btnW = 68;
            int btnH = 22;
            int bx = ix - btnW / 2;
            int by = iy - btnH / 2;

            int fill = hover
                    ? ColorUtil.withAlpha(theme.accent(), 0.45f)
                    : ColorUtil.withAlpha(0xFF121216, 0.92f);
            ctx.fillRoundedRect(bx, by, btnW, btnH, PrimeDesign.RADIUS_SM, fill);
            ctx.fillRoundedBorder(bx, by, btnW, btnH, PrimeDesign.RADIUS_SM, 1,
                    ColorUtil.withAlpha(theme.accent(), hover ? 0.9f : 0.4f), fill);

            String label = SLICE_ICONS[i] + " " + SLICE_LABELS[i];
            int textW = ctx.smoothTextWidth(label, 0.68f);
            ctx.drawSmoothText(label, bx + (btnW - textW) / 2, by + 5, hover ? theme.foreground() : theme.foregroundMuted(), 0.68f);
        }
    }
}
