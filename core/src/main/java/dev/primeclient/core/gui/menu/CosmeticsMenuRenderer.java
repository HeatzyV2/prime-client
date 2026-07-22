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

/** In-client cosmetics inventory — cape + wings only (world-rendered). */
public final class CosmeticsMenuRenderer {

    private static final CosmeticType[] SLOTS = {CosmeticType.CAPE, CosmeticType.WINGS};

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
        for (CosmeticType type : SLOTS) {
            int tw = GuiLayout.tabWidth(ctx, PrimeLang.enumValue(type), 10);
            boolean sel = type == slot;
            ctx.fillRoundedRect(tabX, y + 26, tw, 14, PrimeDesign.RADIUS_SM,
                    sel ? theme.surfaceElevated() : theme.backgroundLight());
            GuiLayout.label(ctx, PrimeLang.enumValue(type), tabX + 4, y + 29, sel ? theme.accent() : theme.foregroundMuted());
            tabX += tw + 4;
        }

        int rowY = y + 48;
        ctx.pushClip(x + 4, rowY, w - 8, h - 80);
        int shown = 0;
        for (CosmeticItem item : cosmetics.catalog().values()) {
            if (item.type() != slot) {
                continue;
            }
            if (shown++ < scrollIndex) {
                continue;
            }
            if (rowY > y + h - 40) {
                break;
            }
            boolean equipped = cosmetics.equipped(slot) != null
                    && cosmetics.equipped(slot).id().equals(item.id());
            ctx.fillRoundedRect(x + 8, rowY, w - 16, 22, PrimeDesign.RADIUS_SM,
                    equipped ? theme.surfaceElevated() : theme.backgroundLight());
            // Larger tint swatch as texture preview stand-in
            ctx.fillRect(x + 12, rowY + 3, 16, 16, item.tintArgb());
            ctx.fillRect(x + 12, rowY + 3, 16, 2, theme.accent());
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, item.name(), w - 110),
                    x + 34, rowY + 4, theme.foreground());
            GuiLayout.label(ctx, PrimeLang.enumValue(item.rarity()), x + w - 72, rowY + 4, theme.foregroundMuted());
            if (equipped) {
                GuiLayout.label(ctx, PrimeLang.get("prime.gui.cosmetics.equipped", "Equipped"),
                        x + 34, rowY + 13, theme.accent());
            }
            rowY += 24;
        }
        ctx.popClip();

        GuiLayout.label(ctx,
                PrimeLang.get("prime.gui.cosmetics.footer", "Visible to you + Prime peers · Click to equip"),
                x + 12, y + h - 16, theme.foregroundMuted());
    }

    public boolean mousePressed(RenderContext ctx, double mx, double my, int screenW, int screenH, CosmeticManager cosmetics) {
        int w = 320;
        int h = 220;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        int tabX = x + 8;
        for (CosmeticType type : SLOTS) {
            int tw = GuiLayout.tabWidth(ctx, PrimeLang.enumValue(type), 10);
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
            if (mx >= x + 8 && mx < x + w - 8 && my >= rowY && my < rowY + 22) {
                CosmeticItem current = cosmetics.equipped(slot);
                if (current != null && current.id().equals(item.id())) {
                    cosmetics.unequip(slot);
                } else {
                    cosmetics.equip(slot, item.id());
                }
                return true;
            }
            rowY += 24;
        }
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public boolean scroll(double delta) {
        scrollIndex = Math.max(0, scrollIndex - (int) delta);
        return true;
    }
}
