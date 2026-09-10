package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleManager;
import dev.primeclient.core.profile.ProfileManager;
import dev.primeclient.core.state.RadialMenuState;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/** In-game radial quick wheel menu for profile switching and shortcuts. */
public final class RadialMenuRenderer {

    private static final String[] SLICE_LABELS = {
            "PVP Profile", "Survival", "Social Hub", "ClickGUI", "Stream Mode", "Settings"
    };

    private static final String[] SLICE_ICONS = {
            "⚔", "🌾", "💬", "⚙", "🎥", "✦"
    };

    private static final int RADIUS = 90;
    private static final int INNER_RADIUS = 32;

    private RadialMenuRenderer() {
    }

    public static void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        if (!RadialMenuState.open()) {
            return;
        }
        int cx = ctx.screenWidth() / 2;
        int cy = ctx.screenHeight() / 2;

        ctx.fillRect(0, 0, ctx.screenWidth(), ctx.screenHeight(), 0x90000000);

        ctx.fillSoftShadow(cx - INNER_RADIUS, cy - INNER_RADIUS, INNER_RADIUS * 2, INNER_RADIUS * 2,
                INNER_RADIUS, 0x80000000);
        ctx.fillRoundedBorder(cx - INNER_RADIUS, cy - INNER_RADIUS, INNER_RADIUS * 2, INNER_RADIUS * 2,
                INNER_RADIUS, 1,
                ColorUtil.withAlpha(theme.accent(), 0.85f), ColorUtil.withAlpha(0xFF0C0C0E, 0.95f));
        ctx.drawSmoothText("PRIME", cx - ctx.smoothTextWidth("PRIME", 0.72f) / 2, cy - 3, theme.accent(), 0.72f);

        int hover = hoveredSlice(mouseX, mouseY, cx, cy);
        int count = SLICE_LABELS.length;
        double sliceAngle = 360.0 / count;

        for (int i = 0; i < count; i++) {
            boolean selected = i == hover;
            double midAngle = Math.toRadians(i * sliceAngle + sliceAngle / 2);
            int itemDist = 62;
            int ix = cx + (int) Math.round(Math.cos(midAngle) * itemDist);
            int iy = cy + (int) Math.round(Math.sin(midAngle) * itemDist);

            int btnW = 68;
            int btnH = 22;
            int bx = ix - btnW / 2;
            int by = iy - btnH / 2;

            int fill = selected
                    ? ColorUtil.withAlpha(theme.accent(), 0.45f)
                    : ColorUtil.withAlpha(0xFF121216, 0.92f);
            ctx.fillRoundedRect(bx, by, btnW, btnH, PrimeDesign.RADIUS_SM, fill);
            ctx.fillRoundedBorder(bx, by, btnW, btnH, PrimeDesign.RADIUS_SM, 1,
                    ColorUtil.withAlpha(theme.accent(), selected ? 0.9f : 0.4f), fill);

            String label = SLICE_ICONS[i] + " " + SLICE_LABELS[i];
            int textW = ctx.smoothTextWidth(label, 0.68f);
            ctx.drawSmoothText(label, bx + (btnW - textW) / 2, by + 5,
                    selected ? theme.foreground() : theme.foregroundMuted(), 0.68f);
        }
    }

    /**
     * Handles a click while the wheel is open. Returns {@code true} if the click was consumed.
     */
    public static boolean mousePressed(double mouseX, double mouseY, int screenW, int screenH,
                                       ProfileManager profiles, MinecraftAdapter adapter,
                                       ModuleManager modules) {
        if (!RadialMenuState.open()) {
            return false;
        }
        int slice = hoveredSlice(mouseX, mouseY, screenW / 2, screenH / 2);
        applySlice(slice, profiles, adapter, modules);
        RadialMenuState.setOpen(false);
        return true;
    }

    private static void applySlice(int slice, ProfileManager profiles, MinecraftAdapter adapter,
                                   ModuleManager modules) {
        if (slice < 0) {
            return;
        }
        switch (slice) {
            case 0 -> {
                if (profiles != null) {
                    profiles.switchTo("pvp");
                }
            }
            case 1 -> {
                if (profiles != null) {
                    profiles.switchTo("survival");
                }
            }
            case 2 -> {
                if (adapter != null) {
                    adapter.openSocialHub();
                }
            }
            case 3 -> {
                if (adapter != null) {
                    adapter.openClickGui();
                }
            }
            case 4 -> {
                if (modules != null) {
                    Module stream = modules.get("stream-privacy-suite");
                    if (stream != null) {
                        stream.toggle();
                    }
                }
            }
            case 5 -> {
                if (adapter != null) {
                    adapter.openPrimeSettings();
                }
            }
            default -> {
            }
        }
    }

    static int hoveredSlice(double mouseX, double mouseY, int cx, int cy) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < INNER_RADIUS || dist > RADIUS + 15) {
            return -1;
        }
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) {
            angle += 360;
        }
        int count = SLICE_LABELS.length;
        double sliceAngle = 360.0 / count;
        int index = (int) (angle / sliceAngle);
        if (index < 0 || index >= count) {
            return -1;
        }
        return index;
    }
}
