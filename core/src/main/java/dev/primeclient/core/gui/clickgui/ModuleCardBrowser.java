package dev.primeclient.core.gui.clickgui;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.FavoritesManager;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.gui.RecentlyUsedManager;
import dev.primeclient.core.gui.TooltipRenderer;
import dev.primeclient.core.gui.UiChrome;
import dev.primeclient.core.gui.component.ToggleWidget;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.module.ModuleManager;
import dev.primeclient.core.module.Setting;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Card-based module browser — ClickGUI V3 (Overview / Recent / Favorites / categories). */
public final class ModuleCardBrowser {

    public static final int CARD_W = 118;
    public static final int CARD_H = 42;
    public static final int TAB_H = PrimeDesign.TAB_HEIGHT + 4;
    public static final int TAB_PAD = 22;

    private static final int TITLE_X = 8;
    private static final int TEXT_PAD = 6;
    private static final int TOGGLE_TOP = 10;

    enum Tab {
        OVERVIEW,
        RECENT,
        FAVORITES,
        CATEGORY
    }

    private final ModuleManager modules;
    private final FavoritesManager favorites;
    private final RecentlyUsedManager recent;
    private final Map<Module, ToggleWidget> toggles = new IdentityHashMap<>();

    private Tab activeTab = Tab.OVERVIEW;
    private ModuleCategory activeCategory = ModuleCategory.PVP;
    private Module selected;
    private String searchQuery = "";
    private float scrollY;
    private float targetScrollY;
    private TooltipRenderer tooltips;
    private Consumer<Module> onModuleOpened = m -> {};

    public ModuleCardBrowser(ModuleManager modules, FavoritesManager favorites, RecentlyUsedManager recent) {
        this.modules = modules;
        this.favorites = favorites;
        this.recent = recent;
    }

    public void setTooltips(TooltipRenderer tooltips) {
        this.tooltips = tooltips;
    }

    public void setOnModuleOpened(Consumer<Module> onModuleOpened) {
        this.onModuleOpened = onModuleOpened != null ? onModuleOpened : m -> {};
    }

    public void setSearchQuery(String query) {
        String next = query == null ? "" : query;
        if (!next.equals(searchQuery)) {
            searchQuery = next;
            targetScrollY = 0;
            scrollY = 0;
        }
    }

    public String searchQuery() {
        return searchQuery;
    }

    public Module selected() {
        return selected;
    }

    public void tick(float deltaSeconds) {
        scrollY = PrimeDesign.animate(scrollY, targetScrollY, deltaSeconds, PrimeDesign.MOTION_NORMAL);
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
        if (list.isEmpty()) {
            String empty;
            if (!searchQuery.isEmpty()) {
                empty = "No results for \"" + searchQuery + "\"";
            } else if (activeTab == Tab.FAVORITES) {
                empty = "No favorites yet — middle-click a card to pin";
            } else if (activeTab == Tab.RECENT) {
                empty = "Recently used modules appear here";
            } else {
                empty = "No modules here";
            }
            ctx.drawUiText(empty, x + PrimeDesign.SPACE_MD, contentY + PrimeDesign.SPACE_LG,
                    theme.foregroundMuted());
            return;
        }
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
        tabX = drawTab(ctx, theme, tabX, y, mouseX, mouseY, "Overview", "◈",
                theme.accent(), activeTab == Tab.OVERVIEW);
        tabX = drawTab(ctx, theme, tabX, y, mouseX, mouseY, "Recent", "↻",
                theme.accent(), activeTab == Tab.RECENT);
        tabX = drawTab(ctx, theme, tabX, y, mouseX, mouseY, "Favorites", "★",
                theme.accent(), activeTab == Tab.FAVORITES);
        for (ModuleCategory cat : ModuleCategory.values()) {
            boolean active = activeTab == Tab.CATEGORY && cat == activeCategory;
            tabX = drawTab(ctx, theme, tabX, y, mouseX, mouseY,
                    cat.displayName(), cat.icon(), cat.accent(), active);
            if (tabX > x + width) {
                break;
            }
        }
    }

    private int drawTab(RenderContext ctx, Theme theme, int tabX, int y,
                        double mouseX, double mouseY, String label, String icon,
                        int accent, boolean active) {
        int tw = ctx.uiTextWidth(label) + TAB_PAD;
        boolean hover = mouseX >= tabX && mouseX < tabX + tw && mouseY >= y && mouseY < y + TAB_H;
        int fill = active ? theme.surfaceElevated()
                : hover ? ColorUtil.withAlpha(theme.backgroundLight(), 0.95f)
                : ColorUtil.withAlpha(theme.backgroundLight(), 0.55f);
        ctx.fillRoundedRect(tabX, y, tw, TAB_H, PrimeDesign.RADIUS_SM, fill);
        if (active) {
            int lineW = Math.max(10, tw - 8);
            ctx.fillRect(tabX + 4, y + TAB_H - 2, lineW, 2, accent);
        } else if (hover) {
            ctx.fillRect(tabX + 4, y + TAB_H - 1, tw - 8, 1, ColorUtil.withAlpha(accent, 0.5f));
        }
        GuiLayout.label(ctx, icon, tabX + 5, y + 5, accent);
        GuiLayout.label(ctx, label, tabX + 17, y + 5,
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

        ctx.fillRect(x + 2, y + 6, 2, CARD_H - 12, module.category().accent());

        int toggleX = cardToggleX(x);
        int toggleY = cardToggleY(y);
        int textMax = toggleX - (x + TITLE_X) - 4;

        String name = module.name();
        String desc = module.description();
        String trimmedName = GuiLayout.trimToWidth(ctx, name, textMax);
        String trimmedDesc = GuiLayout.trimToWidth(ctx, desc, textMax);
        GuiLayout.label(ctx, trimmedName, x + TITLE_X, y + 7, theme.foreground());
        GuiLayout.label(ctx, trimmedDesc, x + TITLE_X, y + 22, theme.foregroundMuted());

        if (hover && tooltips != null) {
            String state = module.isEnabled() ? "Enabled" : "Disabled";
            String tipTitle = name + " · " + state;
            String tipDetail = desc;
            if (favorites.isFavorite(module.id())) {
                tipDetail = "★ Favorite — " + tipDetail;
            }
            tooltips.show(tipTitle, tipDetail, x, Math.max(4, y - 28));
        }

        ToggleWidget toggle = toggles.computeIfAbsent(module, m -> new ToggleWidget());
        toggle.render(ctx, theme, toggleX, toggleY, module.isEnabled());

        if (favorites.isFavorite(module.id())) {
            GuiLayout.label(ctx, "★", x + 6, y + CARD_H - 12, theme.accent());
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
                    return true;
                }
                if (button == 0) {
                    int tx = cardToggleX(cx);
                    int ty = cardToggleY(cy);
                    if (mouseX >= tx && mouseX < tx + PrimeDesign.TOGGLE_WIDTH
                            && mouseY >= ty && mouseY < ty + PrimeDesign.TOGGLE_HEIGHT) {
                        module.toggle();
                        recent.touch(module.id());
                        return true;
                    }
                    selectModule(module);
                    return true;
                }
                selectModule(module);
                return true;
            }
        }
        if (button == 0) {
            selected = null;
        }
        return false;
    }

    private void selectModule(Module module) {
        selected = module;
        recent.touch(module.id());
        onModuleOpened.accept(module);
    }

    private boolean pressTab(RenderContext ctx, double mouseX, double mouseY, int x, int y, int width) {
        int tabX = x;
        String[] fixed = {"Overview", "Recent", "Favorites"};
        Tab[] fixedTabs = {Tab.OVERVIEW, Tab.RECENT, Tab.FAVORITES};
        for (int i = 0; i < fixed.length; i++) {
            int tw = ctx.uiTextWidth(fixed[i]) + TAB_PAD;
            if (mouseX >= tabX && mouseX < tabX + tw) {
                activeTab = fixedTabs[i];
                resetScroll();
                return true;
            }
            tabX += tw + PrimeDesign.SPACE_XS;
        }
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
            recent.touch(module.id());
        }
    }

    void showFavoritesTab() {
        activeTab = Tab.FAVORITES;
        resetScroll();
    }

    void showOverviewTab() {
        activeTab = Tab.OVERVIEW;
        resetScroll();
    }

    void showRecentTab() {
        activeTab = Tab.RECENT;
        resetScroll();
    }

    List<Module> filteredModules() {
        List<Module> source = new ArrayList<>();
        switch (activeTab) {
            case OVERVIEW -> {
                for (ModuleCategory cat : ModuleCategory.values()) {
                    source.addAll(modules.byCategory(cat));
                }
            }
            case RECENT -> source.addAll(recent.resolve(modules));
            case FAVORITES -> source.addAll(favorites.resolve(modules));
            case CATEGORY -> source.addAll(modules.byCategory(activeCategory));
        }
        if (searchQuery.isEmpty()) {
            return source;
        }
        String q = searchQuery.toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : source) {
            if (matches(m, q)) {
                out.add(m);
            }
        }
        // Global search: if category/recent/fav filter is empty, fall back to all modules.
        if (out.isEmpty() && activeTab != Tab.OVERVIEW) {
            for (ModuleCategory cat : ModuleCategory.values()) {
                for (Module m : modules.byCategory(cat)) {
                    if (matches(m, q) && !out.contains(m)) {
                        out.add(m);
                    }
                }
            }
        }
        return out;
    }

    private static boolean matches(Module m, String q) {
        if (m.name().toLowerCase(Locale.ROOT).contains(q)
                || m.description().toLowerCase(Locale.ROOT).contains(q)
                || m.id().toLowerCase(Locale.ROOT).contains(q)
                || m.category().displayName().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (Setting setting : m.settings()) {
            if (setting.name().toLowerCase(Locale.ROOT).contains(q)
                    || setting.id().toLowerCase(Locale.ROOT).contains(q)
                    || setting.description().toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
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
