package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.cloud.CloudSyncManager;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.design.PrimeLogo;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.gui.UiChrome;
import dev.primeclient.core.i18n.PrimeLang;
import dev.primeclient.core.keybind.KeyNames;
import dev.primeclient.core.keybind.Keybind;
import dev.primeclient.core.keybind.KeybindManager;
import dev.primeclient.core.profile.ProfileManager;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Full settings hub with searchable categories. */
public final class SettingsMenuRenderer {

    private static final int ROW_HEIGHT = 16;
    private static final int KEY_BTN_W = 88;

    public enum Category {
        GENERAL("General"),
        APPEARANCE("Appearance"),
        PERFORMANCE("Performance"),
        CONTROLS("Controls"),
        ACCOUNT("Account"),
        PRIVACY("Privacy"),
        ABOUT("About");

        final String label;
        final String key;

        Category(String label) {
            this.label = label;
            this.key = "prime.gui.settings.tab." + name().toLowerCase();
        }

        String translated() {
            return PrimeLang.get(key, label);
        }
    }

    private Category active = Category.GENERAL;
    private final StringBuilder search = new StringBuilder();
    private int controlsScroll;
    private Keybind listeningFor;

    public Category active() {
        return active;
    }

    public boolean capturingKey() {
        return listeningFor != null;
    }

    public boolean captureKey(int glfwKey, KeybindManager keybinds) {
        if (listeningFor == null) {
            return false;
        }
        if (glfwKey == 256) {
            listeningFor = null;
            return true;
        }
        if (glfwKey == 259 || glfwKey == 261) {
            keybinds.rebind(listeningFor, Keybind.UNBOUND);
            listeningFor = null;
            return true;
        }
        keybinds.rebind(listeningFor, glfwKey);
        listeningFor = null;
        return true;
    }

    public boolean scroll(double delta) {
        if (active != Category.CONTROLS) {
            return false;
        }
        controlsScroll = Math.max(0, controlsScroll - (int) delta);
        return true;
    }

    public void render(RenderContext ctx, Theme theme, ThemeManager themes, ProfileManager profiles,
                       CloudSyncManager cloud, MinecraftAdapter adapter, KeybindManager keybinds,
                       int screenW, int screenH, double mouseX, double mouseY) {
        int panelW = 340;
        int panelH = 240;
        int x = (screenW - panelW) / 2;
        int y = (screenH - panelH) / 2;
        UiChrome.glassPanel(ctx, theme, x, y, panelW, panelH);
        GuiLayout.label(ctx, PrimeLang.get("prime.gui.settings.title", "Settings"), x + 12, y + 10, theme.accent());

        int tabY = y + 28;
        int tabX = x + 8;
        int tabsInRow = 0;
        ctx.pushClip(x + 4, tabY, panelW - 8, 34);
        for (Category cat : Category.values()) {
            if (!matchesSearch(cat.translated())) {
                continue;
            }
            boolean sel = cat == active;
            int tw = GuiLayout.tabWidth(ctx, cat.translated(), 10);
            if (tabsInRow >= 4) {
                tabX = x + 8;
                tabY += 16;
                tabsInRow = 0;
            }
            ctx.fillRoundedRect(tabX, tabY, tw, 14, PrimeDesign.RADIUS_SM,
                    sel ? theme.surfaceElevated() : theme.backgroundLight());
            GuiLayout.label(ctx, cat.translated(), tabX + 5, tabY + 3, sel ? theme.accent() : theme.foregroundMuted());
            tabX += tw + 4;
            tabsInRow++;
        }
        ctx.popClip();

        int rowY = tabY + 22;
        int contentBottom = y + panelH - 28;
        ctx.pushClip(x + 4, rowY, panelW - 8, contentBottom - rowY);
        switch (active) {
            case GENERAL -> {
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.profile", "Profile"), profiles.activeProfile());
                rowY += 16;
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.minecraft", "Minecraft"), adapter.minecraftVersion());
                rowY += 16;
                String sync = cloud.autoSync()
                        ? PrimeLang.get("prime.gui.settings.cloud_sync.enabled", "Enabled")
                        : PrimeLang.get("prime.gui.settings.cloud_sync.disabled", "Disabled");
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.cloud_sync", "Cloud sync"), sync);
            }
            case APPEARANCE -> {
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.active_theme", "Active theme"), themes.active().name());
                rowY += 18;
                ctx.fillRoundedRect(x + 12, rowY, 100, 16, PrimeDesign.RADIUS_SM, theme.backgroundLight());
                GuiLayout.label(ctx, PrimeLang.get("prime.gui.settings.theme.dark", "Prime Dark"),
                        x + 18, rowY + 4, theme.foreground());
                ctx.fillRoundedRect(x + 120, rowY, 100, 16, PrimeDesign.RADIUS_SM, theme.backgroundLight());
                GuiLayout.label(ctx, PrimeLang.get("prime.gui.settings.theme.light", "Prime Light"),
                        x + 126, rowY + 4, theme.foreground());
            }
            case PERFORMANCE -> row(ctx, theme, x + 12, rowY,
                    PrimeLang.get("prime.gui.settings.row.tip", "Tip"),
                    PrimeLang.get("prime.gui.settings.tip.performance", "Use Performance Profiles module"));
            case CONTROLS -> renderControls(ctx, theme, keybinds, x, rowY, panelW, contentBottom);
            case ACCOUNT -> {
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.player", "Player"), adapter.playerName());
                rowY += 16;
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.prime_account", "Prime Account"),
                        PrimeLang.get("prime.gui.settings.account.hint", "Connect via Prime Account module"));
            }
            case PRIVACY -> row(ctx, theme, x + 12, rowY,
                    PrimeLang.get("prime.gui.settings.row.data", "Data"),
                    PrimeLang.get("prime.gui.settings.privacy.hint", "Configs stored locally only"));
            case ABOUT -> {
                PrimeLogo.draw(ctx, x + 12, rowY, 14, 0xFFFFFFFF);
                row(ctx, theme, x + 12 + PrimeLogo.widthForHeight(14) + 6, rowY + 2,
                        PrimeLang.get("prime.gui.settings.about.client", "Prime Client"),
                        PrimeLang.get("prime.gui.settings.about.version", "v%s", PrimeDesign.VERSION));
                rowY += 20;
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.about.legitimate", "Legitimate client"),
                        PrimeLang.get("prime.gui.settings.about.tagline", "Visual & QoL only"));
            }
        }
        ctx.popClip();

        String footer = active == Category.CONTROLS
                ? PrimeLang.get("prime.gui.settings.keybinds.footer",
                "Click key to rebind · Backspace clears · Scroll for more")
                : (search.isEmpty()
                ? PrimeLang.get("prime.gui.settings.search.placeholder", "Search settings...")
                : search.toString());
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, footer, panelW - 24),
                x + 12, y + panelH - 18, theme.foregroundMuted());
    }

    private void renderControls(RenderContext ctx, Theme theme, KeybindManager keybinds,
                                int x, int rowY, int panelW, int contentBottom) {
        List<Keybind> binds = filteredKeybinds(keybinds);
        int maxScroll = Math.max(0, binds.size() - visibleControlRows(contentBottom - rowY));
        controlsScroll = Math.min(controlsScroll, maxScroll);

        int drawY = rowY;
        int skipped = 0;
        for (Keybind bind : binds) {
            if (skipped++ < controlsScroll) {
                continue;
            }
            if (drawY + ROW_HEIGHT > contentBottom) {
                break;
            }
            boolean listening = bind == listeningFor;
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, bind.displayName(), 130),
                    x + 12, drawY + 3, theme.foreground());
            int btnX = x + panelW - KEY_BTN_W - 20;
            ctx.fillRoundedRect(btnX, drawY, KEY_BTN_W, 14, PrimeDesign.RADIUS_SM,
                    listening ? theme.accent() : theme.backgroundLight());
            String label = listening
                    ? PrimeLang.get("prime.gui.settings.keybinds.listening", "Press a key…")
                    : KeyNames.glfwName(bind.key());
            int textColor = listening ? theme.background() : theme.foreground();
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, label, KEY_BTN_W - 8),
                    btnX + 4, drawY + 3, textColor);
            drawY += ROW_HEIGHT;
        }
    }

    public boolean mousePressed(RenderContext ctx, double mx, double my, int screenW, int screenH,
                                ThemeManager themes, KeybindManager keybinds) {
        int panelW = 340;
        int panelH = 240;
        int x = (screenW - panelW) / 2;
        int y = (screenH - panelH) / 2;
        int tabY = y + 28;
        int tabX = x + 8;
        int tabsInRow = 0;
        for (Category cat : Category.values()) {
            if (!matchesSearch(cat.translated())) {
                continue;
            }
            int tw = GuiLayout.tabWidth(ctx, cat.translated(), 10);
            if (tabsInRow >= 4) {
                tabX = x + 8;
                tabY += 16;
                tabsInRow = 0;
            }
            if (mx >= tabX && mx < tabX + tw && my >= tabY && my < tabY + 14) {
                active = cat;
                controlsScroll = 0;
                listeningFor = null;
                return true;
            }
            tabX += tw + 4;
            tabsInRow++;
        }
        if (active == Category.APPEARANCE) {
            int rowY = y + 92;
            if (mx >= x + 12 && mx < x + 112 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-dark");
                return true;
            }
            if (mx >= x + 120 && mx < x + 220 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-light");
                return true;
            }
        }
        if (active == Category.CONTROLS) {
            int rowY = tabY + 22;
            int contentBottom = y + panelH - 28;
            List<Keybind> binds = filteredKeybinds(keybinds);
            int btnX = x + panelW - KEY_BTN_W - 20;
            int drawY = rowY;
            int skipped = 0;
            for (Keybind bind : binds) {
                if (skipped++ < controlsScroll) {
                    continue;
                }
                if (drawY + ROW_HEIGHT > contentBottom) {
                    break;
                }
                if (mx >= btnX && mx < btnX + KEY_BTN_W && my >= drawY && my < drawY + 14) {
                    listeningFor = bind;
                    return true;
                }
                drawY += ROW_HEIGHT;
            }
        }
        return mx >= x && mx < x + panelW && my >= y && my < y + panelH;
    }

    public boolean charTyped(char c) {
        if (capturingKey()) {
            return true;
        }
        if (c < ' ') {
            return false;
        }
        search.append(c);
        controlsScroll = 0;
        return true;
    }

    public boolean keyPressed(int key) {
        if (capturingKey()) {
            return true;
        }
        if (key == 259 && !search.isEmpty()) {
            search.setLength(search.length() - 1);
            controlsScroll = 0;
            return true;
        }
        return false;
    }

    private List<Keybind> filteredKeybinds(KeybindManager keybinds) {
        List<Keybind> list = new ArrayList<>(keybinds.all());
        list.sort(Comparator.comparing(Keybind::category).thenComparing(Keybind::displayName));
        if (search.isEmpty()) {
            return list;
        }
        String needle = search.toString().toLowerCase();
        return list.stream()
                .filter(bind -> bind.displayName().toLowerCase().contains(needle)
                        || bind.category().toLowerCase().contains(needle)
                        || KeyNames.glfwName(bind.key()).toLowerCase().contains(needle))
                .toList();
    }

    private static int visibleControlRows(int clipHeight) {
        return Math.max(1, clipHeight / ROW_HEIGHT);
    }

    private boolean matchesSearch(String label) {
        if (search.isEmpty()) {
            return true;
        }
        return label.toLowerCase().contains(search.toString().toLowerCase());
    }

    private static void row(RenderContext ctx, Theme theme, int x, int y, String k, String v) {
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, k, 110), x, y, theme.foreground());
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, v, 180), x + 120, y, theme.foregroundMuted());
    }
}
