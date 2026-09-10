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
import dev.primeclient.core.module.ModuleManager;
import dev.primeclient.core.modules.performance.PerformanceProfilesModule;
import dev.primeclient.core.modules.performance.PerformanceProfilesModule.Profile;
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

    private static int settingsPanelW(int screenW) {
        return Math.min(420, Math.max(360, screenW - 80));
    }

    private static int settingsPanelH(int screenH) {
        return Math.min(300, Math.max(260, screenH - 60));
    }

    public void render(RenderContext ctx, Theme theme, ThemeManager themes, ProfileManager profiles,
                       CloudSyncManager cloud, MinecraftAdapter adapter, KeybindManager keybinds,
                       ModuleManager modules, int screenW, int screenH, double mouseX, double mouseY) {
        int panelW = settingsPanelW(screenW);
        int panelH = settingsPanelH(screenH);
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
                GuiLayout.label(ctx, PrimeLang.get("prime.gui.settings.row.profile", "Profile"),
                        x + 12, rowY, theme.foreground());
                rowY += 14;
                rowY = drawProfileChips(ctx, theme, profiles, x + 12, rowY, panelW - 24, mouseX, mouseY);
                rowY += 6;
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.minecraft", "Minecraft"), adapter.minecraftVersion());
                rowY += 16;
                String sync = cloud.autoSync()
                        ? PrimeLang.get("prime.gui.settings.cloud_sync.enabled", "Enabled")
                        : PrimeLang.get("prime.gui.settings.cloud_sync.disabled", "Disabled");
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.cloud_sync", "Local backup"), sync);
            }
            case APPEARANCE -> {
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.active_theme", "Active theme"), themes.active().name());
                rowY += 18;
                drawThemeChip(ctx, theme, themes, "prime-crimson",
                        PrimeLang.get("prime.gui.settings.theme.crimson", "Crimson"),
                        x + 12, rowY, 100);
                drawThemeChip(ctx, theme, themes, "prime-midnight",
                        PrimeLang.get("prime.gui.settings.theme.midnight", "Midnight"),
                        x + 118, rowY, 100);
                drawThemeChip(ctx, theme, themes, "prime-aurora",
                        PrimeLang.get("prime.gui.settings.theme.aurora", "Aurora"),
                        x + 224, rowY, 100);
                rowY += 20;
                drawThemeChip(ctx, theme, themes, "prime-obsidian",
                        PrimeLang.get("prime.gui.settings.theme.obsidian", "Obsidian"),
                        x + 12, rowY, 100);
                drawThemeChip(ctx, theme, themes, "prime-ember",
                        PrimeLang.get("prime.gui.settings.theme.ember", "Ember"),
                        x + 118, rowY, 100);
                drawThemeChip(ctx, theme, themes, "prime-violet",
                        PrimeLang.get("prime.gui.settings.theme.violet", "Violet"),
                        x + 224, rowY, 100);
                rowY += 20;
                drawThemeChip(ctx, theme, themes, "prime-emerald",
                        PrimeLang.get("prime.gui.settings.theme.emerald", "Emerald"),
                        x + 12, rowY, 100);
            }
            case PERFORMANCE -> {
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.profile_preset", "Preset"),
                        "");
                rowY += 18;
                PerformanceProfilesModule perf = performanceProfiles(modules);
                Profile selected = (perf != null && perf.isEnabled()) ? perf.currentProfile() : null;
                drawProfileChip(ctx, theme, selected == Profile.LOW,
                        PrimeLang.get("prime.gui.settings.perf.low", "LOW"),
                        x + 12, rowY, 100);
                drawProfileChip(ctx, theme, selected == Profile.MEDIUM,
                        PrimeLang.get("prime.gui.settings.perf.med", "MED"),
                        x + 118, rowY, 100);
                drawProfileChip(ctx, theme, selected == Profile.HIGH,
                        PrimeLang.get("prime.gui.settings.perf.high", "HIGH"),
                        x + 224, rowY, 100);
            }
            case CONTROLS -> renderControls(ctx, theme, keybinds, x, rowY, panelW, contentBottom);
            case ACCOUNT -> {
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.player", "Player"), adapter.playerName());
                rowY += 16;
                String type = adapter.sessionAccountType();
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.session", "Session"),
                        type == null || type.isBlank() ? "—" : type);
                rowY += 22;
                boolean canSwitch = !adapter.isInGame();
                boolean hover = canSwitch && mouseX >= x + 12 && mouseX < x + 168
                        && mouseY >= rowY && mouseY < rowY + 20;
                UiChrome.button(ctx, theme, x + 12, rowY, 156, 20, hover, canSwitch);
                String btn = PrimeLang.get("prime.gui.settings.account.switch", "Switch account…");
                GuiLayout.label(ctx, btn, x + 20, rowY + 6,
                        canSwitch ? theme.foreground() : theme.foregroundMuted());
                rowY += 26;
                GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx,
                                canSwitch
                                        ? PrimeLang.get("prime.gui.settings.account.switch_hint",
                                        "Uses accounts from the Prime Launcher")
                                        : PrimeLang.get("prime.gui.settings.account.in_world",
                                        "Return to the title screen to switch accounts"),
                                panelW - 40),
                        x + 12, rowY, theme.foregroundMuted());
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
                                ThemeManager themes, KeybindManager keybinds, MinecraftAdapter adapter,
                                ModuleManager modules, ProfileManager profiles) {
        int panelW = settingsPanelW(screenW);
        int panelH = settingsPanelH(screenH);
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
        if (active == Category.GENERAL && profiles != null) {
            if (handleProfileChipClick(ctx, profiles, mx, my, x + 12, tabY + 22 + 14, panelW - 24)) {
                return true;
            }
        }
        if (active == Category.APPEARANCE) {
            int rowY = tabY + 40;
            if (mx >= x + 12 && mx < x + 112 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-crimson");
                return true;
            }
            if (mx >= x + 118 && mx < x + 218 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-midnight");
                return true;
            }
            if (mx >= x + 224 && mx < x + 324 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-aurora");
                return true;
            }
            int row2 = rowY + 20;
            if (mx >= x + 12 && mx < x + 112 && my >= row2 && my < row2 + 16) {
                themes.setActive("prime-obsidian");
                return true;
            }
            if (mx >= x + 118 && mx < x + 218 && my >= row2 && my < row2 + 16) {
                themes.setActive("prime-ember");
                return true;
            }
            if (mx >= x + 224 && mx < x + 324 && my >= row2 && my < row2 + 16) {
                themes.setActive("prime-violet");
                return true;
            }
            int row3 = row2 + 20;
            if (mx >= x + 12 && mx < x + 112 && my >= row3 && my < row3 + 16) {
                themes.setActive("prime-emerald");
                return true;
            }
        }
        if (active == Category.PERFORMANCE) {
            PerformanceProfilesModule perf = performanceProfiles(modules);
            if (perf != null) {
                int rowY = tabY + 22 + 18;
                if (mx >= x + 12 && mx < x + 112 && my >= rowY && my < rowY + 16) {
                    perf.applyPreset(Profile.LOW);
                    return true;
                }
                if (mx >= x + 118 && mx < x + 218 && my >= rowY && my < rowY + 16) {
                    perf.applyPreset(Profile.MEDIUM);
                    return true;
                }
                if (mx >= x + 224 && mx < x + 324 && my >= rowY && my < rowY + 16) {
                    perf.applyPreset(Profile.HIGH);
                    return true;
                }
            }
        }
        if (active == Category.ACCOUNT && adapter != null && !adapter.isInGame()) {
            int rowY = tabY + 22 + 16 + 22;
            if (mx >= x + 12 && mx < x + 168 && my >= rowY && my < rowY + 20) {
                adapter.openAccountSwitcher();
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

    private static void drawThemeChip(RenderContext ctx, Theme theme, ThemeManager themes,
                                      String themeId, String label, int x, int y, int w) {
        boolean selected = themeId.equals(themes.active().id());
        ctx.fillRoundedRect(x, y, w, 16, PrimeDesign.RADIUS_SM,
                selected ? theme.accent() : theme.backgroundLight());
        GuiLayout.label(ctx, label, x + 6, y + 4,
                selected ? theme.foreground() : theme.foregroundMuted());
    }

    private static void drawProfileChip(RenderContext ctx, Theme theme, boolean selected,
                                        String label, int x, int y, int w) {
        ctx.fillRoundedRect(x, y, w, 16, PrimeDesign.RADIUS_SM,
                selected ? theme.accent() : theme.backgroundLight());
        GuiLayout.label(ctx, label, x + 6, y + 4,
                selected ? theme.foreground() : theme.foregroundMuted());
    }

    /** Draws profile name chips + New…; returns the Y past the last row. */
    private static int drawProfileChips(RenderContext ctx, Theme theme, ProfileManager profiles,
                                        int x, int y, int maxW, double mouseX, double mouseY) {
        int chipX = x;
        int chipY = y;
        String active = profiles.activeProfile();
        for (String name : profiles.listProfiles()) {
            int w = Math.max(48, Math.min(120, GuiLayout.labelWidth(ctx, name) + 12));
            if (chipX + w > x + maxW) {
                chipX = x;
                chipY += 18;
            }
            boolean selected = name.equals(active);
            boolean hover = mouseX >= chipX && mouseX < chipX + w && mouseY >= chipY && mouseY < chipY + 16;
            ctx.fillRoundedRect(chipX, chipY, w, 16, PrimeDesign.RADIUS_SM,
                    selected ? theme.accent() : (hover ? theme.surfaceElevated() : theme.backgroundLight()));
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, name, w - 8), chipX + 6, chipY + 4,
                    selected ? theme.foreground() : theme.foregroundMuted());
            chipX += w + 4;
        }
        String newLabel = PrimeLang.get("prime.gui.settings.profile.new", "New…");
        int newW = Math.max(48, GuiLayout.labelWidth(ctx, newLabel) + 12);
        if (chipX + newW > x + maxW) {
            chipX = x;
            chipY += 18;
        }
        boolean newHover = mouseX >= chipX && mouseX < chipX + newW
                && mouseY >= chipY && mouseY < chipY + 16;
        ctx.fillRoundedRect(chipX, chipY, newW, 16, PrimeDesign.RADIUS_SM,
                newHover ? theme.surfaceElevated() : theme.backgroundLight());
        GuiLayout.label(ctx, newLabel, chipX + 6, chipY + 4, theme.accent());
        return chipY + 16;
    }

    private static boolean handleProfileChipClick(RenderContext ctx, ProfileManager profiles,
                                                  double mx, double my, int x, int y, int maxW) {
        int chipX = x;
        int chipY = y;
        for (String name : profiles.listProfiles()) {
            int w = Math.max(48, Math.min(120, GuiLayout.labelWidth(ctx, name) + 12));
            if (chipX + w > x + maxW) {
                chipX = x;
                chipY += 18;
            }
            if (mx >= chipX && mx < chipX + w && my >= chipY && my < chipY + 16) {
                profiles.switchTo(name);
                return true;
            }
            chipX += w + 4;
        }
        String newLabel = PrimeLang.get("prime.gui.settings.profile.new", "New…");
        int newW = Math.max(48, GuiLayout.labelWidth(ctx, newLabel) + 12);
        if (chipX + newW > x + maxW) {
            chipX = x;
            chipY += 18;
        }
        if (mx >= chipX && mx < chipX + newW && my >= chipY && my < chipY + 16) {
            String created = createUniqueProfile(profiles);
            profiles.create(created);
            profiles.switchTo(created);
            return true;
        }
        return false;
    }

    private static String createUniqueProfile(ProfileManager profiles) {
        String base = "profile-" + (System.currentTimeMillis() / 1000L);
        if (!profiles.listProfiles().contains(base)) {
            return base;
        }
        for (int i = 2; i < 100; i++) {
            String candidate = base + "-" + i;
            if (candidate.length() <= 32 && !profiles.listProfiles().contains(candidate)) {
                return candidate;
            }
        }
        return "profile-" + Long.toString(System.currentTimeMillis(), 36);
    }

    private static PerformanceProfilesModule performanceProfiles(ModuleManager modules) {
        if (modules == null) {
            return null;
        }
        var module = modules.get("performance-profiles");
        return module instanceof PerformanceProfilesModule profiles ? profiles : null;
    }
}
