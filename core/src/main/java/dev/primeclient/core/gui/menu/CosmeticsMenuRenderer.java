package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.cosmetics.CosmeticItem;
import dev.primeclient.core.cosmetics.CosmeticManager;
import dev.primeclient.core.cosmetics.CosmeticType;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.gui.UiChrome;
import dev.primeclient.core.i18n.PrimeLang;
import dev.primeclient.core.theme.Theme;

/** In-client cosmetics inventory browser. */
public final class CosmeticsMenuRenderer {

    private CosmeticType slot = CosmeticType.CAPE;
    private int scrollIndex;

    public void render(RenderContext ctx, Theme theme, CosmeticManager cosmetics,
                       int screenW, int screenH, double mouseX, double mouseY) {
        int w = 320;
        int h = 220;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        UiChrome.glassPanel(ctx, theme, x, y, w, h);
        GuiLayout.label(ctx, PrimeLang.get("prime.gui.cosmetics.title", "Cosmetics"), x + 12, y + 10, theme.accent());

        int tabX = x + 8;
        ctx.pushClip(x + 4, y + 26, w - 8, 14);
        for (CosmeticType type : CosmeticType.values()) {
            int tw = GuiLayout.tabWidth(ctx, PrimeLang.enumValue(type), 10);
            boolean sel = type == slot;
            ctx.fillRoundedRect(tabX, y + 26, tw, 14, PrimeDesign.RADIUS_SM,
                    sel ? theme.surfaceElevated() : theme.backgroundLight());
            GuiLayout.label(ctx, PrimeLang.enumValue(type), tabX + 4, y + 29, sel ? theme.accent() : theme.foregroundMuted());
            tabX += tw + 4;
        }
        ctx.popClip();

        int rowY = y + 48;
        ctx.pushClip(x + 4, rowY, w - 8, h - 68);
        int shown = 0;
        for (CosmeticItem item : cosmetics.catalog().values()) {
            if (item.type() != slot) {
                continue;
            }
            if (shown++ < scrollIndex) {
                continue;
            }
            if (rowY > y + h - 24) {
                break;
            }
            boolean equipped = cosmetics.equipped(slot) != null
                    && cosmetics.equipped(slot).id().equals(item.id());
            ctx.fillRoundedRect(x + 8, rowY, w - 16, 18, PrimeDesign.RADIUS_SM,
                    equipped ? theme.surfaceElevated() : theme.backgroundLight());
            ctx.fillRect(x + 10, rowY + 4, 10, 10, item.tintArgb());
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, item.name(), w - 100),
                    x + 24, rowY + 5, theme.foreground());
            GuiLayout.label(ctx, PrimeLang.enumValue(item.rarity()), x + w - 70, rowY + 5, theme.foregroundMuted());
            rowY += 20;
        }
        ctx.popClip();

        GuiLayout.label(ctx, PrimeLang.get("prime.gui.cosmetics.footer", "Click to equip · Scroll for more"),
                x + 12, y + h - 16, theme.foregroundMuted());
    }

    public boolean mousePressed(RenderContext ctx, double mx, double my, int screenW, int screenH, CosmeticManager cosmetics) {
        int w = 320;
        int h = 220;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        int tabX = x + 8;
        for (CosmeticType type : CosmeticType.values()) {
            int tw = GuiLayout.tabWidth(ctx, type.name(), 10);
            if (mx >= tabX && mx < tabX + tw && my >= y + 26 && my < y + 40) {
                slot = type;
                scrollIndex = 0;
                return true;
            }
            tabX += tw + 4;
        }
        int rowY = y + 48;
        for (CosmeticItem item : cosmetics.catalog().values()) {
            if (item.type() != slot) {
                continue;
            }
            if (mx >= x + 8 && mx < x + w - 8 && my >= rowY && my < rowY + 18) {
                cosmetics.equip(slot, item.id());
                return true;
            }
            rowY += 20;
        }
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public boolean scroll(double delta) {
        scrollIndex = Math.max(0, scrollIndex - (int) delta);
        return true;
    }
}
