package dev.primeclient.core.gui.clickgui;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.FavoritesManager;
import dev.primeclient.core.gui.component.ToggleWidget;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.module.ModuleManager;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.Easing;

import java.util.ArrayList;
import java.util.List;

/** Card-based module browser with category tabs. */
public final class ModuleCardBrowser {

    public static final int CARD_W = 156;
    public static final int CARD_H = 54;
    public static final int TAB_H = 20;
    public static final int COLS = 4;

    private final ModuleManager modules;
    private final FavoritesManager favorites;
    private final ToggleWidget toggleAnim = new ToggleWidget();

    private ModuleCategory activeCategory = ModuleCategory.PVP;
    private Module selected;
    private float scrollY;
    private float targetScrollY;

    public ModuleCardBrowser(ModuleManager modules, FavoritesManager favorites) {
        this.modules = modules;
        this.favorites = favorites;
    }

    public Module selected() {
        return selected;
    }

    public void tick(float deltaSeconds) {
        scrollY = Easing.lerp(scrollY, targetScrollY, deltaSeconds * 14f);
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, int width, int height,
                       double mouseX, double mouseY) {
        renderTabs(ctx, theme, x, y, width, mouseX, mouseY);
        int contentY = y + TAB_H + PrimeDesign.SPACE_SM;
        int contentH = height - TAB_H - PrimeDesign.SPACE_SM;
        List<Module> list = filteredModules();
        int cols = Math.max(1, width / (CARD_W + PrimeDesign.SPACE_SM));
        int rows = (list.size() + cols - 1) / cols;
        int totalH = rows * (CARD_H + PrimeDesign.SPACE_SM);
        int startRow = Math.max(0, (int) (scrollY / (CARD_H + PrimeDesign.SPACE_SM)));

        for (int i = startRow * cols; i < list.size(); i++) {
            Module module = list.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (CARD_W + PrimeDesign.SPACE_SM);
            int cy = contentY + row * (CARD_H + PrimeDesign.SPACE_SM) - Math.round(scrollY);
            if (cy + CARD_H < contentY || cy > contentY + contentH) {
                continue;
            }
            renderCard(ctx, theme, module, cx, cy, mouseX, mouseY);
        }
    }

    private void renderTabs(RenderContext ctx, Theme theme, int x, int y, int width,
                            double mouseX, double mouseY) {
        int tabX = x;
        for (ModuleCategory cat : ModuleCategory.values()) {
            int tw = ctx.textWidth(cat.displayName()) + 16;
            boolean active = cat == activeCategory;
            boolean hover = mouseX >= tabX && mouseX < tabX + tw && mouseY >= y && mouseY < y + TAB_H;
            ctx.fillRect(tabX, y, tw, TAB_H, active ? theme.surfaceElevated() : theme.backgroundLight());
            if (active || hover) {
                ctx.fillRect(tabX, y + TAB_H - 2, tw, 2, cat.accent());
            }
            ctx.drawText(cat.icon(), tabX + 4, y + 5, cat.accent(), true);
            ctx.drawText(cat.displayName(), tabX + 16, y + 5, theme.foreground(), true);
            tabX += tw + 4;
        }
    }

    private void renderCard(RenderContext ctx, Theme theme, Module module, int x, int y,
                            double mouseX, double mouseY) {
        boolean hover = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
        boolean sel = module == selected;
        ctx.fillRect(x, y, CARD_W, CARD_H, sel ? theme.surfaceElevated() : theme.background());
        if (hover || sel) {
            ctx.fillRect(x, y, CARD_W, 2, module.category().accent());
        }
        ctx.fillRect(x + PrimeDesign.SPACE_SM, y + PrimeDesign.SPACE_SM, 14, 14, module.category().accent());
        ctx.drawText(module.category().icon(), x + PrimeDesign.SPACE_SM + 3, y + PrimeDesign.SPACE_SM + 2,
                theme.foreground(), true);
        ctx.drawText(trim(ctx, module.name(), CARD_W - 40), x + 24, y + 6, theme.foreground(), true);
        ctx.drawText(trim(ctx, module.description(), CARD_W - 12), x + 8, y + 22, theme.foregroundMuted(), true);
        toggleAnim.tick(module.isEnabled(), 1f / 20f);
        toggleAnim.render(ctx, theme, x + CARD_W - PrimeDesign.TOGGLE_WIDTH - 6, y + 6, module.isEnabled());
        if (favorites.isFavorite(module.id())) {
            ctx.drawText("*", x + CARD_W - 14, y + CARD_H - 12, theme.accent(), true);
        }
    }

    public boolean mousePressed(double mouseX, double mouseY, int x, int y, int width, int height, int button) {
        if (mouseY >= y && mouseY < y + TAB_H) {
            int tabX = x;
            for (ModuleCategory cat : ModuleCategory.values()) {
                int tw = 60;
                if (mouseX >= tabX && mouseX < tabX + tw) {
                    activeCategory = cat;
                    targetScrollY = 0;
                    scrollY = 0;
                    return true;
                }
                tabX += tw + 4;
            }
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
                    int tx = cx + CARD_W - PrimeDesign.TOGGLE_WIDTH - 6;
                    if (mouseX >= tx && mouseX < tx + PrimeDesign.TOGGLE_WIDTH && mouseY >= cy + 4 && mouseY < cy + 18) {
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

    public boolean mouseScrolled(double amount) {
        targetScrollY = Math.max(0, targetScrollY - (float) amount * 20f);
        return true;
    }

    /** Pre-selects a module for headless GUI tests. */
    void selectForTests(Module module) {
        selected = module;
        if (module != null) {
            activeCategory = module.category();
        }
    }

    private List<Module> filteredModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : modules.byCategory(activeCategory)) {
            list.add(m);
        }
        return list;
    }

    private static String trim(RenderContext ctx, String text, int maxW) {
        if (ctx.textWidth(text) <= maxW) {
            return text;
        }
        for (int i = text.length() - 1; i > 0; i--) {
            String s = text.substring(0, i) + "…";
            if (ctx.textWidth(s) <= maxW) {
                return s;
            }
        }
        return text;
    }
}
