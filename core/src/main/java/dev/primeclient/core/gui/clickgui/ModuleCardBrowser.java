package dev.primeclient.core.gui.clickgui;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.FavoritesManager;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.gui.TooltipRenderer;
import dev.primeclient.core.gui.UiChrome;
import dev.primeclient.core.gui.component.ToggleWidget;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.module.ModuleManager;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Card-based module browser with Overview / Favorites / category tabs. */
public final class ModuleCardBrowser {

    public static final int CARD_W = 112;
    public static final int CARD_H = 38;
    public static final int TAB_H = 18;
    public static final int TAB_PAD = 26;

    private static final int TITLE_X = 8;
    private static final int TEXT_PAD = 6;
    private static final int TOGGLE_TOP = 8;

    enum Tab {
        OVERVIEW,
        FAVORITES,
        CATEGORY
    }

    private final ModuleManager modules;
    private final FavoritesManager favorites;
    private final Map<Module, ToggleWidget> toggles = new IdentityHashMap<>();

    private Tab activeTab = Tab.OVERVIEW;
    private ModuleCategory activeCategory = ModuleCategory.PVP;
    private Module selected;
    private String searchQuery = "";
    private float scrollY;
    private float targetScrollY;
    private TooltipRenderer tooltips;

    public ModuleCardBrowser(ModuleManager modules, FavoritesManager favorites) {
        this.modules = modules;
        this.favorites = favorites;
    }

    public void setTooltips(TooltipRenderer tooltips) {
        this.tooltips = tooltips;
    }

    public void setSearchQuery(String query) {
        String next = query == null ? "" : query;
        if (!next.equals(searchQuery)) {
            searchQuery = next;
            targetScrollY = 0;
            scrollY = 0;
        }
    }

    public Module selected() {
        return selected;
    }

    public void tick(float deltaSeconds) {
        scrollY = PrimeDesign.animate(scrollY, targetScrollY, deltaSeconds, 14f);
        for (var entry : toggles.entrySet()) {
            entry.getValue().tick(entry.getKey().isEnabled(), deltaSeconds);
        }
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, int width, int height,
                       double mouseX, double mouseY) {
        ctx.pushClip(x, y, width, TAB_H);
        renderTabs(ctx, theme, x, y, width, mouseX, mouseY);
        ctx.popClip();

        int contentY = y + TAB_H + PrimeDesign.SPACE_SM;
        int contentH = height - TAB_H - PrimeDesign.SPACE_SM;
        List<Module> list = filteredModules();
        int cols = Math.max(1, width / (CARD_W + PrimeDesign.SPACE_SM));
        int rows = (list.size() + cols - 1) / cols;
        int totalH = rows * (CARD_H + PrimeDesign.SPACE_SM);
        int maxScroll = Math.max(0, totalH - contentH);
        if (targetScrollY > maxScroll) {
            targetScrollY = maxScroll;
        }
        if (scrollY > maxScroll) {
            scrollY = maxScroll;
        }

        ctx.pushClip(x, contentY, width, contentH);
        int startRow = Math.max(0, (int) (scrollY / (CARD_H + PrimeDesign.SPACE_SM)));
        int endRow = Math.min(rows, startRow + (contentH / (CARD_H + PrimeDesign.SPACE_SM)) + 2);
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < cols; col++) {
                int i = row * cols + col;
                if (i >= list.size()) {
                    break;
                }
                Module module = list.get(i);
                int cx = x + col * (CARD_W + PrimeDesign.SPACE_SM);
                int cy = contentY + row * (CARD_H + PrimeDesign.SPACE_SM) - Math.round(scrollY);
                if (cy + CARD_H < contentY || cy > contentY + contentH) {
                    continue;
                }
                renderCard(ctx, theme, module, cx, cy, mouseX, mouseY);
            }
        }
        ctx.popClip();
    }

    private void renderTabs(RenderContext ctx, Theme theme, int x, int y, int width,
                            double mouseX, double mouseY) {
        int tabX = x;
        tabX = drawTab(ctx, theme, tabX, y, width, mouseX, mouseY, "Overview", "◈",
                theme.accent(), activeTab == Tab.OVERVIEW);
        tabX = drawTab(ctx, theme, tabX, y, width, mouseX, mouseY, "Favorites", "★",
                theme.accent(), activeTab == Tab.FAVORITES);
        for (ModuleCategory cat : ModuleCategory.values()) {
            boolean active = activeTab == Tab.CATEGORY && cat == activeCategory;
            tabX = drawTab(ctx, theme, tabX, y, width, mouseX, mouseY,
                    cat.displayName(), cat.icon(), cat.accent(), active);
            if (tabX > x + width) {
                break;
            }
        }
    }

    private int drawTab(RenderContext ctx, Theme theme, int tabX, int y, int width,
                        double mouseX, double mouseY, String label, String icon,
                        int accent, boolean active) {
        int tw = ctx.uiTextWidth(label) + TAB_PAD;
        if (tabX > width + (tabX - tw)) {
            // still draw if partially visible
        }
        boolean hover = mouseX >= tabX && mouseX < tabX + tw && mouseY >= y && mouseY < y + TAB_H;
        int fill = active ? theme.surfaceElevated() : theme.backgroundLight();
        ctx.fillRoundedRect(tabX, y, tw, TAB_H, PrimeDesign.RADIUS_SM, fill);
        if (active || hover) {
            ctx.fillRect(tabX + 2, y + TAB_H - 2, tw - 4, 1, accent);
        }
        GuiLayout.label(ctx, icon, tabX + 6, y + 4, accent);
        GuiLayout.label(ctx, label, tabX + 18, y + 4,
                active ? theme.foreground() : theme.foregroundMuted());
        return tabX + tw + PrimeDesign.SPACE_XS;
    }

    private void renderCard(RenderContext ctx, Theme theme, Module module, int x, int y,
                            double mouseX, double mouseY) {
        boolean hover = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
        boolean sel = module == selected;
        UiChrome.cardLite(ctx, theme, x, y, CARD_W, CARD_H, sel || hover);
        if (hover || sel) {
            ctx.fillRect(x + 2, y + CARD_H - 2, CARD_W - 4, 1,
                    ColorUtil.withAlpha(module.category().accent(), sel ? 0.95f : 0.55f));
        }

        ctx.fillRect(x + 2, y + 5, 2, CARD_H - 10, module.category().accent());

        int toggleX = cardToggleX(x);
        int toggleY = cardToggleY(y);
        int textMax = toggleX - (x + TITLE_X) - 4;

        String name = module.name();
        String desc = module.description();
        String trimmedName = GuiLayout.trimToWidth(ctx, name, textMax);
        String trimmedDesc = GuiLayout.trimToWidth(ctx, desc, textMax);
        GuiLayout.label(ctx, trimmedName, x + TITLE_X, y + 6, theme.foreground());
        GuiLayout.label(ctx, trimmedDesc, x + TITLE_X, y + 20, theme.foregroundMuted());

        if (hover && tooltips != null && (!trimmedName.equals(name) || !trimmedDesc.equals(desc))) {
            String tip = name.equals(trimmedName) ? desc : name + " — " + desc;
            tooltips.show(tip, x, y - 16);
        }

        ToggleWidget toggle = toggles.computeIfAbsent(module, m -> new ToggleWidget());
        toggle.render(ctx, theme, toggleX, toggleY, module.isEnabled());

        if (favorites.isFavorite(module.id())) {
            GuiLayout.label(ctx, "★", x + 6, y + CARD_H - 11, theme.accent());
        }
    }

    public boolean mousePressed(RenderContext ctx, double mouseX, double mouseY, int x, int y,
                                int width, int height, int button) {
        if (mouseY >= y && mouseY < y + TAB_H) {
            return pressTab(ctx, mouseX, mouseY, x, y, width);
        }
        List<Module> list = filteredModules();
        int cols = Math.max(1, width / (CARD_W + PrimeDesign.SPACE_SM));
        int contentY = y + TAB_H + PrimeDesign.SPACE_SM;
        for (int i = 0; i < list.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (CARD_W + PrimeDesign.SPACE_SM);
            int cy = contentY + row * (CARD_H + PrimeDesign.SPACE_SM) - Math.round(scrollY);
            if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                Module module = list.get(i);
                if (button == 2) {
                    favorites.toggle(module.id());
                } else if (button == 0) {
                    int tx = cardToggleX(cx);
                    int ty = cardToggleY(cy);
                    if (mouseX >= tx && mouseX < tx + PrimeDesign.TOGGLE_WIDTH
                            && mouseY >= ty && mouseY < ty + PrimeDesign.TOGGLE_HEIGHT) {
                        module.toggle();
                    } else {
                        selected = module;
                    }
                } else {
                    selected = module;
                }
                return true;
            }
        }
        return false;
    }

    private boolean pressTab(RenderContext ctx, double mouseX, double mouseY, int x, int y, int width) {
        int tabX = x;
        int overviewW = ctx.uiTextWidth("Overview") + TAB_PAD;
        if (mouseX >= tabX && mouseX < tabX + overviewW) {
            activeTab = Tab.OVERVIEW;
            resetScroll();
            return true;
        }
        tabX += overviewW + PrimeDesign.SPACE_XS;
        int favW = ctx.uiTextWidth("Favorites") + TAB_PAD;
        if (mouseX >= tabX && mouseX < tabX + favW) {
            activeTab = Tab.FAVORITES;
            resetScroll();
            return true;
        }
        tabX += favW + PrimeDesign.SPACE_XS;
        for (ModuleCategory cat : ModuleCategory.values()) {
            int tw = tabWidth(ctx, cat);
            if (tabX > x + width) {
                break;
            }
            if (mouseX >= tabX && mouseX < tabX + tw) {
                activeTab = Tab.CATEGORY;
                activeCategory = cat;
                resetScroll();
                return true;
            }
            tabX += tw + PrimeDesign.SPACE_XS;
        }
        return false;
    }

    private void resetScroll() {
        targetScrollY = 0;
        scrollY = 0;
    }

    public boolean mouseScrolled(double amount, int x, int y, int width, int height) {
        int contentH = height - TAB_H - PrimeDesign.SPACE_SM;
        List<Module> list = filteredModules();
        int cols = Math.max(1, width / (CARD_W + PrimeDesign.SPACE_SM));
        int rows = (list.size() + cols - 1) / cols;
        int totalH = rows * (CARD_H + PrimeDesign.SPACE_SM);
        int maxScroll = Math.max(0, totalH - contentH);
        targetScrollY = GuiLayout.clamp(Math.round(targetScrollY - (float) amount * 24f), 0, maxScroll);
        return true;
    }

    /** Pre-selects a module for headless GUI tests. */
    void selectForTests(Module module) {
        selected = module;
        if (module != null) {
            activeTab = Tab.CATEGORY;
            activeCategory = module.category();
        }
    }

    List<Module> filteredModules() {
        List<Module> source = new ArrayList<>();
        switch (activeTab) {
            case OVERVIEW -> {
                for (ModuleCategory cat : ModuleCategory.values()) {
                    source.addAll(modules.byCategory(cat));
                }
            }
            case FAVORITES -> source.addAll(favorites.resolve(modules));
            case CATEGORY -> source.addAll(modules.byCategory(activeCategory));
        }
        if (searchQuery.isEmpty()) {
            return source;
        }
        String q = searchQuery.toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : source) {
            if (m.name().toLowerCase(Locale.ROOT).contains(q)
                    || m.description().toLowerCase(Locale.ROOT).contains(q)
                    || m.id().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(m);
            }
        }
        return out;
    }

    private static int cardToggleX(int cardX) {
        return cardX + CARD_W - PrimeDesign.TOGGLE_WIDTH - TEXT_PAD;
    }

    private static int cardToggleY(int cardY) {
        return cardY + TOGGLE_TOP;
    }

    private static int tabWidth(RenderContext ctx, ModuleCategory cat) {
        if (ctx == null) {
            return cat.displayName().length() * 6 + TAB_PAD;
        }
        return ctx.uiTextWidth(cat.displayName()) + TAB_PAD;
    }
}
