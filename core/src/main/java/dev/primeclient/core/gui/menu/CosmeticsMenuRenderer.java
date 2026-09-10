package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.cosmetics.CollectionProgress;
import dev.primeclient.core.cosmetics.CosmeticItem;
import dev.primeclient.core.cosmetics.CosmeticManager;
import dev.primeclient.core.cosmetics.CosmeticType;
import dev.primeclient.core.cosmetics.EmoteCatalog;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.gui.UiChrome;
import dev.primeclient.core.i18n.PrimeLang;
import dev.primeclient.core.state.EmoteState;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/** In-client cosmetics inventory — full Cosmetics Update UI. */
public final class CosmeticsMenuRenderer {

    private static final CosmeticType[] SLOTS = {
            CosmeticType.CAPE, CosmeticType.WINGS, CosmeticType.AURA, CosmeticType.TRAIL,
            CosmeticType.HAT, CosmeticType.EMOTE, CosmeticType.BADGE
    };
    private static final int PANEL_W = 520;
    private static final int PANEL_H = 300;
    private static final int PREVIEW_W = 120;

    private CosmeticType slot = CosmeticType.CAPE;
    private boolean collectionView;
    private int scrollIndex;
    private float previewPulse;

    public void render(RenderContext ctx, Theme theme, CosmeticManager cosmetics,
                       int screenW, int screenH, double mouseX, double mouseY) {
        previewPulse = (previewPulse + 0.04f) % ((float) (Math.PI * 2));
        int w = PANEL_W;
        int h = PANEL_H;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        UiChrome.glassPanel(ctx, theme, x, y, w, h);
        GuiLayout.label(ctx, PrimeLang.get("prime.gui.cosmetics.title", "Cosmetics"), x + 12, y + 8, theme.accent());

        int tabX = x + 8;
        int tabY = y + 22;
        for (CosmeticType type : SLOTS) {
            String label = shortTab(type);
            int tw = Math.max(36, GuiLayout.tabWidth(ctx, label, 8));
            boolean sel = !collectionView && type == slot;
            ctx.fillRoundedRect(tabX, tabY, tw, 14, PrimeDesign.RADIUS_SM,
                    sel ? theme.surfaceElevated() : theme.backgroundLight());
            GuiLayout.label(ctx, label, tabX + 4, tabY + 3, sel ? theme.accent() : theme.foregroundMuted());
            tabX += tw + 3;
        }
        int cw = GuiLayout.tabWidth(ctx, "Coll.", 8);
        boolean collSel = collectionView;
        ctx.fillRoundedRect(tabX, tabY, cw, 14, PrimeDesign.RADIUS_SM,
                collSel ? theme.surfaceElevated() : theme.backgroundLight());
        GuiLayout.label(ctx, "Coll.", tabX + 4, tabY + 3, collSel ? theme.accent() : theme.foregroundMuted());

        if (collectionView) {
            renderCollection(ctx, theme, cosmetics, x, y, w, h);
            return;
        }

        renderShowroom(ctx, theme, cosmetics, x, y, w, h);
        renderList(ctx, theme, cosmetics, x, y, w, h, mouseX, mouseY);

        GuiLayout.label(ctx,
                PrimeLang.get("prime.gui.cosmetics.footer",
                        "Prime peers · Click equip · ★ favorite · /emotes"),
                x + 12, y + h - 14, theme.foregroundMuted());
    }

    private void renderShowroom(RenderContext ctx, Theme theme, CosmeticManager cosmetics,
                                int x, int y, int w, int h) {
        int previewX = x + w - PREVIEW_W - 14;
        int previewY = y + 44;
        int previewH = h - 68;
        UiChrome.card(ctx, theme, previewX, previewY, PREVIEW_W, previewH, true);

        CosmeticItem equipped = cosmetics.equipped(slot);
        if (slot == CosmeticType.EMOTE) {
            EmoteState.ActiveEmote active = EmoteState.local();
            if (active != null) {
                equipped = cosmetics.catalog().get(active.emoteId());
            }
        }
        int tint = equipped != null ? equipped.tintArgb() : ColorUtil.withAlpha(theme.backgroundLight(), 0.9f);
        int auraColor = equipped != null
                ? ColorUtil.withAlpha(equipped.tintArgb(), 0.35f + 0.15f * (float) Math.sin(previewPulse))
                : ColorUtil.withAlpha(theme.accent(), 0.2f);

        // Pedestal + silhouette showroom (better than tint-only card)
        ctx.fillSoftShadow(previewX + 8, previewY + 12, PREVIEW_W - 16, previewH - 48, 10, auraColor);
        int bodyX = previewX + PREVIEW_W / 2 - 14;
        int bodyY = previewY + 28;
        ctx.fillRoundedRect(bodyX + 6, bodyY, 16, 16, 8, ColorUtil.withAlpha(0xFFFFFFFF, 0.85f)); // head
        ctx.fillRoundedRect(bodyX + 2, bodyY + 16, 24, 34, 4, tint); // torso
        ctx.fillRoundedRect(bodyX - 4, bodyY + 18, 8, 22, 3, ColorUtil.withAlpha(tint, 0.85f)); // L arm
        ctx.fillRoundedRect(bodyX + 24, bodyY + 18, 8, 22, 3, ColorUtil.withAlpha(tint, 0.85f)); // R arm
        ctx.fillRoundedRect(bodyX + 4, bodyY + 48, 8, 22, 3, ColorUtil.withAlpha(0xFF1E293B, 0.9f)); // L leg
        ctx.fillRoundedRect(bodyX + 16, bodyY + 48, 8, 22, 3, ColorUtil.withAlpha(0xFF1E293B, 0.9f)); // R leg

        // Cape / wings / hat accents
        if (equipped != null) {
            if (equipped.type() == CosmeticType.CAPE || equipped.type() == CosmeticType.WINGS) {
                ctx.fillRoundedRect(bodyX + 26, bodyY + 14, 10, 40, 3, ColorUtil.withAlpha(tint, 0.7f));
            }
            if (equipped.type() == CosmeticType.HAT) {
                ctx.fillRoundedRect(bodyX + 4, bodyY - 8, 20, 8, 3, tint);
            }
            if (equipped.type() == CosmeticType.AURA || equipped.type() == CosmeticType.TRAIL) {
                ctx.fillRoundedBorder(bodyX - 6, bodyY + 10, 40, 56, 8, 1, auraColor, 0);
            }
            if (equipped.type() == CosmeticType.BADGE) {
                ctx.fillRoundedRect(bodyX + 28, bodyY + 20, 10, 10, 5, tint);
            }
        }

        ctx.fillRect(previewX + 14, previewY + previewH - 28, PREVIEW_W - 28, 2, theme.accent());
        String previewLabel = equipped != null
                ? GuiLayout.trimToWidth(ctx, equipped.name(), PREVIEW_W - 12)
                : PrimeLang.get("prime.gui.cosmetics.none", "None");
        GuiLayout.label(ctx, previewLabel, previewX + 8, previewY + previewH - 20, theme.foreground());
        if (equipped != null) {
            GuiLayout.label(ctx, rarityLabel(equipped.rarity()), previewX + 8, previewY + previewH - 10,
                    rarityColor(theme, equipped.rarity()));
        }
    }

    private void renderList(RenderContext ctx, Theme theme, CosmeticManager cosmetics,
                            int x, int y, int w, int h, double mouseX, double mouseY) {
        int listW = w - PREVIEW_W - 28;
        int rowY = y + 44;
        ctx.pushClip(x + 4, rowY, listW, h - 66);
        int shown = 0;
        CosmeticItem equipped = cosmetics.equipped(slot);

        if (slot == CosmeticType.EMOTE) {
            for (var entry : EmoteCatalog.all().entrySet()) {
                CosmeticItem item = cosmetics.catalog().get(entry.getKey());
                if (item == null) {
                    continue;
                }
                if (shown++ < scrollIndex) {
                    continue;
                }
                if (rowY > y + h - 36) {
                    break;
                }
                boolean hover = mouseX >= x + 8 && mouseX < x + 8 + listW - 8
                        && mouseY >= rowY && mouseY < rowY + 26;
                UiChrome.cardLite(ctx, theme, x + 8, rowY, listW - 8, 26, hover);
                ctx.fillRoundedRect(x + 12, rowY + 5, 16, 16, 4, item.tintArgb());
                GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, item.name(), listW - 100),
                        x + 34, rowY + 5, theme.foreground());
                GuiLayout.label(ctx, rarityLabel(item.rarity()), x + listW - 78, rowY + 5,
                        rarityColor(theme, item.rarity()));
                if (cosmetics.isFavorite(item.id())) {
                    GuiLayout.label(ctx, "★", x + listW - 20, rowY + 5, 0xFFFBBF24);
                }
                GuiLayout.label(ctx, "Play", x + 34, rowY + 14, theme.accent());
                rowY += 28;
            }
        } else {
            for (CosmeticItem item : cosmetics.catalog().values()) {
                if (item.type() != slot) {
                    continue;
                }
                if ("cape-prime".equals(item.id())) {
                    continue; // hide legacy alias from list
                }
                if (shown++ < scrollIndex) {
                    continue;
                }
                if (rowY > y + h - 36) {
                    break;
                }
                boolean isEquipped = equipped != null && equipped.id().equals(item.id());
                boolean hover = mouseX >= x + 8 && mouseX < x + 8 + listW - 8
                        && mouseY >= rowY && mouseY < rowY + 26;
                UiChrome.cardLite(ctx, theme, x + 8, rowY, listW - 8, 26, isEquipped || hover);
                ctx.fillRoundedRect(x + 12, rowY + 5, 16, 16, 4, item.tintArgb());
                ctx.fillRect(x + 12, rowY + 5, 16, 2, rarityColor(theme, item.rarity()));
                GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, item.name(), listW - 110),
                        x + 34, rowY + 5, theme.foreground());
                GuiLayout.label(ctx, rarityLabel(item.rarity()), x + listW - 78, rowY + 5,
                        rarityColor(theme, item.rarity()));
                if (cosmetics.isFavorite(item.id())) {
                    GuiLayout.label(ctx, "★", x + listW - 20, rowY + 5, 0xFFFBBF24);
                }
                if (isEquipped) {
                    GuiLayout.label(ctx, PrimeLang.get("prime.gui.cosmetics.equipped", "Equipped"),
                            x + 34, rowY + 14, theme.accent());
                } else if (!item.description().isBlank()) {
                    GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, item.description(), listW - 100),
                            x + 34, rowY + 14, theme.foregroundMuted());
                }
                rowY += 28;
            }
        }
        ctx.popClip();
    }

    private void renderCollection(RenderContext ctx, Theme theme, CosmeticManager cosmetics,
                                  int x, int y, int w, int h) {
        CollectionProgress progress = cosmetics.collectionProgress();
        GuiLayout.label(ctx,
                String.format("Collection  %.0f%%  (%d / %d)",
                        progress.overallPercent(), progress.ownedTotal(), progress.catalogTotal()),
                x + 16, y + 48, theme.foreground());

        int rowY = y + 70;
        for (CosmeticType type : CosmeticType.values()) {
            CollectionProgress.TypeStats stats = progress.forType(type);
            if (stats.total() == 0) {
                continue;
            }
            GuiLayout.label(ctx, type.name(), x + 16, rowY, theme.foregroundMuted());
            int barW = w - 160;
            ctx.fillRoundedRect(x + 90, rowY + 2, barW, 8, 4, theme.backgroundLight());
            int fill = (int) (barW * (stats.percent() / 100f));
            if (fill > 0) {
                ctx.fillRoundedRect(x + 90, rowY + 2, fill, 8, 4, theme.accent());
            }
            GuiLayout.label(ctx, stats.owned() + "/" + stats.total(), x + w - 50, rowY, theme.foreground());
            rowY += 22;
        }
        GuiLayout.label(ctx, "All cosmetics unlocked for Prime.", x + 16, y + h - 20, theme.foregroundMuted());
    }

    public boolean mousePressed(RenderContext ctx, double mx, double my, int screenW, int screenH,
                                CosmeticManager cosmetics) {
        int w = PANEL_W;
        int h = PANEL_H;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        int tabX = x + 8;
        int tabY = y + 22;
        for (CosmeticType type : SLOTS) {
            String label = shortTab(type);
            int tw = Math.max(36, GuiLayout.tabWidth(ctx, label, 8));
            if (mx >= tabX && mx < tabX + tw && my >= tabY && my < tabY + 14) {
                slot = type;
                collectionView = false;
                scrollIndex = 0;
                return true;
            }
            tabX += tw + 3;
        }
        int cw = GuiLayout.tabWidth(ctx, "Coll.", 8);
        if (mx >= tabX && mx < tabX + cw && my >= tabY && my < tabY + 14) {
            collectionView = true;
            scrollIndex = 0;
            return true;
        }
        if (collectionView) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        int listW = w - PREVIEW_W - 28;
        int rowY = y + 44;
        int shown = 0;

        if (slot == CosmeticType.EMOTE) {
            for (var entry : EmoteCatalog.all().entrySet()) {
                CosmeticItem item = cosmetics.catalog().get(entry.getKey());
                if (item == null) {
                    continue;
                }
                if (shown++ < scrollIndex) {
                    continue;
                }
                if (mx >= x + 8 && mx < x + 8 + listW - 8 && my >= rowY && my < rowY + 26) {
                    if (mx >= x + listW - 24) {
                        cosmetics.toggleFavorite(item.id());
                    } else {
                        EmoteState.playLocal(item.id());
                    }
                    return true;
                }
                rowY += 28;
                if (rowY > y + h - 36) {
                    break;
                }
            }
        } else {
            for (CosmeticItem item : cosmetics.catalog().values()) {
                if (item.type() != slot || "cape-prime".equals(item.id())) {
                    continue;
                }
                if (shown++ < scrollIndex) {
                    continue;
                }
                if (mx >= x + 8 && mx < x + 8 + listW - 8 && my >= rowY && my < rowY + 26) {
                    if (mx >= x + listW - 24) {
                        cosmetics.toggleFavorite(item.id());
                        return true;
                    }
                    CosmeticItem current = cosmetics.equipped(slot);
                    if (current != null && current.id().equals(item.id())) {
                        cosmetics.unequip(slot);
                    } else {
                        cosmetics.equip(slot, item.id());
                    }
                    return true;
                }
                rowY += 28;
                if (rowY > y + h - 36) {
                    break;
                }
            }
        }
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public boolean scroll(double delta) {
        scrollIndex = Math.max(0, scrollIndex - (int) delta);
        return true;
    }

    private static String shortTab(CosmeticType type) {
        return switch (type) {
            case CAPE -> "Capes";
            case WINGS -> "Wings";
            case AURA -> "Auras";
            case TRAIL -> "Trails";
            case HAT -> "Hats";
            case EMOTE -> "Emotes";
            case BADGE -> "Badges";
        };
    }

    private static String rarityLabel(CosmeticItem.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> "Common";
            case RARE -> "Rare";
            case EPIC -> "Epic";
            case LEGENDARY -> "Legend";
            case MYTHIC -> "Mythic";
            case PRIME_EXCLUSIVE -> "Prime";
        };
    }

    private static int rarityColor(Theme theme, CosmeticItem.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> theme.foregroundMuted();
            case RARE -> 0xFF22C55E;
            case EPIC -> 0xFFA855F7;
            case LEGENDARY -> 0xFFF59E0B;
            case MYTHIC -> 0xFFEC4899;
            case PRIME_EXCLUSIVE -> theme.accent();
        };
    }
}
