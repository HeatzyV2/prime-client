package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.UiChrome;
import dev.primeclient.core.theme.Theme;

/** Shared text button — same look in ClickGUI, menus and HUD chrome. */
public final class ButtonWidget {

    public enum Size {
        SM, MD, LG
    }

    public enum Variant {
        PRIMARY, SECONDARY, GHOST, DANGER
    }

    private float hoverProgress;

    public void tick(boolean hovered, float deltaSeconds) {
        hoverProgress = PrimeDesign.animateMs(hoverProgress, hovered ? 1f : 0f,
                deltaSeconds, PrimeDesign.DURATION_HOVER_MS);
    }

    public int height(Size size) {
        return switch (size) {
            case SM -> PrimeDesign.BUTTON_HEIGHT_SM;
            case MD -> PrimeDesign.BUTTON_HEIGHT_MD;
            case LG -> PrimeDesign.BUTTON_HEIGHT_LG;
        };
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, int w, Size size,
                       Variant variant, String label, boolean hovered, boolean active) {
        int h = height(size);
        boolean primary = variant == Variant.PRIMARY || active;
        if (variant == Variant.GHOST) {
            if (hovered || hoverProgress > 0.05f) {
                ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_MD,
                        theme.backgroundLight());
            }
        } else if (variant == Variant.DANGER) {
            UiChrome.button(ctx, theme, x, y, w, h, hovered || active, false);
            ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_MD,
                    (theme.error() & 0x00FFFFFF) | 0x33000000);
        } else {
            UiChrome.button(ctx, theme, x, y, w, h, hovered || hoverProgress > 0.4f, primary);
        }
        int color = switch (variant) {
            case PRIMARY -> primary || active ? 0xFFFFFFFF : theme.foreground();
            case DANGER -> theme.error();
            case GHOST -> theme.foregroundMuted();
            case SECONDARY -> theme.foreground();
        };
        int textY = y + (h - ctx.uiFontHeight()) / 2;
        ctx.drawUiText(label, x + (w - ctx.uiTextWidth(label)) / 2, textY, color);
    }

    public boolean hit(double mx, double my, int x, int y, int w, Size size) {
        int h = height(size);
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
