package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.cosmetics.CosmeticItem;
import dev.primeclient.core.cosmetics.CosmeticManager;
import dev.primeclient.core.cosmetics.CosmeticType;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;

/** In-client cosmetics inventory browser. */
public final class CosmeticsMenuRenderer {

    private CosmeticType slot = CosmeticType.CAPE;
    private int scrollIndex;

    public void render(RenderContext ctx, Theme theme, CosmeticManager cosmetics,
                       int screenW, int screenH, double mouseX, double mouseY) {
        int w = 300;
        int h = 200;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        ctx.fillRect(x, y, w, h, theme.background());
        ctx.fillRect(x, y, w, 2, theme.accent());
        ctx.drawText("Cosmetics", x + 12, y + 10, theme.accent(), true);

        int tabX = x + 8;
        for (CosmeticType type : CosmeticType.values()) {
            int tw = ctx.textWidth(type.name()) + 10;
            boolean sel = type == slot;
            ctx.fillRect(tabX, y + 26, tw, 14, sel ? theme.surfaceElevated() : theme.backgroundLight());
            ctx.drawText(type.name(), tabX + 4, y + 29, sel ? theme.accent() : theme.foregroundMuted(), true);
            tabX += tw + 4;
        }

        int rowY = y + 48;
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
            ctx.fillRect(x + 8, rowY, w - 16, 18, equipped ? theme.surfaceElevated() : theme.backgroundLight());
            ctx.fillRect(x + 10, rowY + 4, 10, 10, item.tintArgb());
            ctx.drawText(item.name(), x + 24, rowY + 5, theme.foreground(), true);
            ctx.drawText(item.rarity().name(), x + w - 70, rowY + 5, theme.foregroundMuted(), true);
            rowY += 20;
        }
        ctx.drawText("Click item to equip  •  Scroll for more", x + 12, y + h - 16, theme.foregroundMuted(), true);
    }

    public boolean mousePressed(double mx, double my, int screenW, int screenH, CosmeticManager cosmetics) {
        int w = 300;
        int h = 200;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        int tabX = x + 8;
        for (CosmeticType type : CosmeticType.values()) {
            int tw = type.name().length() * 6 + 10;
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
