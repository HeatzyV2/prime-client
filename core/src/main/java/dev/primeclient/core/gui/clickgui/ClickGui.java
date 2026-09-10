package dev.primeclient.core.gui.clickgui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.config.ConfigBinding;
import dev.primeclient.core.gui.FavoritesManager;
import dev.primeclient.core.cloud.CloudSyncManager;
import dev.primeclient.core.cosmetics.CosmeticManager;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.gui.RecentlyUsedManager;
import dev.primeclient.core.gui.menu.MainMenuRenderer;
import dev.primeclient.core.gui.menu.OnboardingManager;
import dev.primeclient.core.gui.menu.OnboardingScreen;
import dev.primeclient.core.gui.component.ColorPickerWidget;
import dev.primeclient.core.gui.component.SearchField;
import dev.primeclient.core.gui.component.TextInputField;
import dev.primeclient.core.module.ColorSetting;
import dev.primeclient.core.module.StringSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.module.ModuleManager;
import dev.primeclient.core.module.Setting;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.profile.ProfileManager;
import dev.primeclient.core.gui.TooltipRenderer;
import dev.primeclient.core.i18n.PrimeLang;
import dev.primeclient.core.gui.menu.ConfigurationsMenuRenderer;
import dev.primeclient.core.gui.menu.CosmeticsMenuRenderer;
import dev.primeclient.core.gui.menu.SettingsMenuRenderer;
import dev.primeclient.core.keybind.KeybindManager;

import dev.primeclient.core.util.Easing;

import java.util.ArrayList;
import java.util.List;

/**
 * Prime ClickGUI V3 — hub menu, card browser, search, favorites, and settings.
 */
public final class ClickGui implements ConfigBinding {

    private static final int CHROME_PAD = PrimeDesign.SPACE_MD;

    private final ModuleManager modules;
    private final ThemeManager themes;
    private final FavoritesManager favorites;
    private final RecentlyUsedManager recent = new RecentlyUsedManager();
    private final MinecraftAdapter adapter;
    private final OnboardingManager onboarding;
    private final CloudSyncManager cloudSync;
    private final CosmeticManager cosmetics;
    private final ProfileManager profiles;
    private final KeybindManager keybinds;
    private final TooltipRenderer tooltips;
    private final MainMenuRenderer mainMenu = new MainMenuRenderer();
    private final ModuleCardBrowser cardBrowser;
    private final SearchField searchField;
    private final SettingsMenuRenderer settingsMenu = new SettingsMenuRenderer();
    private final CosmeticsMenuRenderer cosmeticsMenu = new CosmeticsMenuRenderer();
    private final ConfigurationsMenuRenderer configurationsMenu = new ConfigurationsMenuRenderer();
    private Panel selectedModulePanel;

    private TextInputField stringEditor;
    private StringSetting editingString;
    private ColorPickerWidget colorPicker;
    private ColorSetting editingColor;
    private int editorX;
    private int editorY;

    private final List<Panel> panels = new ArrayList<>();
    private Panel favoritesPanel;
    private int favoritesRevision = Integer.MIN_VALUE;

    private Panel draggingPanel;
    private Panel sliderPanel;
    private Setting sliderSetting;

    private int screenWidth = 800;
    private int screenHeight = 600;
    private int fontHeight = 9;
    private RenderContext textMetrics;
    private ClickGuiView view = ClickGuiView.MAIN_MENU;
    private float openFade;
    private float menuSlide;
    private long lastAnimNanos;
    private Runnable onboardingCompleteHandler;

    public ClickGui(ModuleManager modules, ThemeManager themes,
                    FavoritesManager favorites, MinecraftAdapter adapter,
                    OnboardingManager onboarding, CloudSyncManager cloudSync,
                    CosmeticManager cosmetics, ProfileManager profiles,
                    KeybindManager keybinds, TooltipRenderer tooltips) {
        this.modules = modules;
        this.themes = themes;
        this.favorites = favorites;
        this.adapter = adapter;
        this.onboarding = onboarding;
        this.cloudSync = cloudSync;
        this.cosmetics = cosmetics;
        this.profiles = profiles;
        this.keybinds = keybinds;
        this.tooltips = tooltips;
        this.cardBrowser = new ModuleCardBrowser(modules, favorites, recent);
        this.cardBrowser.setTooltips(tooltips);
        this.searchField = new SearchField(
                PrimeLang.get("prime.gui.clickgui.search.placeholder", "Search modules…"), 48);
        // Legacy panels kept only for config round-trip / test helpers — Browse uses cards.
        float x = 8;
        for (ModuleCategory category : ModuleCategory.values()) {
            panels.add(new Panel(category.displayName(), modules.byCategory(category),
                    favorites, x, 8));
            x += Panel.WIDTH + 8;
        }
        favoritesPanel = new Panel(PrimeLang.get("prime.gui.clickgui.favorites", "Favorites"),
                favorites.resolve(modules), favorites, 8, 8);
        favoritesRevision = favorites.revision();
    }

    public void setOnboardingCompleteHandler(Runnable handler) {
        this.onboardingCompleteHandler = handler;
    }

    public void onOpen() {
        searchField.clear();
        searchField.setFocused(false);
        cardBrowser.setSearchQuery("");
        draggingPanel = null;
        sliderPanel = null;
        sliderSetting = null;
        closeEditors();
        view = onboarding.completed() ? ClickGuiView.MAIN_MENU : ClickGuiView.ONBOARDING;
        openFade = 0f;
        menuSlide = -12f;
    }

    private void closeEditors() {
        stringEditor = null;
        editingString = null;
        colorPicker = null;
        editingColor = null;
    }

    /** For tests: skip the main menu and show category panels. */
    public void showBrowse() {
        view = ClickGuiView.BROWSE;
        cardBrowser.showOverviewTab();
        openFade = 1f;
        menuSlide = 0f;
        searchField.setFocused(true);
    }

    /** Opens ClickGUI on the settings hub (title screen shortcut). */
    public void showSettings() {
        view = ClickGuiView.SETTINGS;
        openFade = 1f;
        menuSlide = 0f;
    }

    /** For tests: select a module card and open its settings panel. */
    public void selectModuleForTests(Module module) {
        cardBrowser.selectForTests(module);
        refreshSelectedPanel();
    }

    /** For tests: interact with a legacy category panel (still persisted in config). */
    public boolean pressCategoryPanel(ModuleCategory category, double mouseX, double mouseY, int button) {
        for (Panel panel : panels) {
            if (panel.title().equals(category.displayName())) {
                return dispatchPress(panel, mouseX, mouseY, button);
            }
        }
        return false;
    }

    /** For tests: drag a legacy category panel header. */
    public void dragCategoryPanel(ModuleCategory category, double mouseX, double mouseY,
                                  int screenW, int screenH) {
        for (Panel panel : panels) {
            if (panel.title().equals(category.displayName())) {
                panel.mouseDragged(mouseX, mouseY, screenW, screenH);
                return;
            }
        }
    }

    public ClickGuiView view() {
        return view;
    }

    /** Top-left Y of the module card grid (below search + tabs). Exposed for layout tests. */
    public int browseContentY() {
        return browserY() + ModuleCardBrowser.TAB_H + PrimeDesign.SPACE_SM;
    }

    public int browserY() {
        return CHROME_PAD + PrimeDesign.SEARCH_HEIGHT + PrimeDesign.SPACE_SM;
    }

    public void tick(float deltaSeconds) {
        float dt = deltaSeconds;
        if (dt <= 0f) {
            dt = frameDeltaSeconds();
        }
        if (PrimeDesign.reducedMotion) {
            openFade = 1f;
            menuSlide = 0f;
        } else {
            openFade = Easing.lerp(openFade, 1f, dt * 8f);
            if (view == ClickGuiView.MAIN_MENU || view == ClickGuiView.ONBOARDING) {
                menuSlide = Easing.lerp(menuSlide, 0f, dt * 10f);
            }
        }
        if (view == ClickGuiView.MAIN_MENU || view == ClickGuiView.ONBOARDING) {
            mainMenu.tick(dt);
        }
        for (Panel panel : panels) {
            panel.tick(dt);
        }
        if (favoritesPanel != null) {
            favoritesPanel.tick(dt);
        }
        searchField.tick(dt);
        cardBrowser.tick(dt);
        refreshSelectedPanel();
        tooltips.tick(Math.max(1, Math.round(dt * 1000f)));
    }

    /** Frame-time delta for render-driven animation (capped). */
    private float frameDeltaSeconds() {
        long now = System.nanoTime();
        float dt = lastAnimNanos == 0L ? 1f / 60f : (now - lastAnimNanos) / 1_000_000_000f;
        lastAnimNanos = now;
        return Math.clamp(dt, 0f, 0.05f);
    }

    private void refreshSelectedPanel() {
        Module sel = cardBrowser.selected();
        if (sel == null) {
            selectedModulePanel = null;
            return;
        }
        if (selectedModulePanel == null || !selectedModulePanel.hasModule(sel)) {
            selectedModulePanel = new Panel(sel.name(), List.of(sel), favorites,
                    screenWidth - Panel.WIDTH - CHROME_PAD, browserY());
            selectedModulePanel.expandModule(sel);
        }
    }

    private boolean isModuleBrowserView() {
        return view == ClickGuiView.BROWSE || view == ClickGuiView.FAVORITES;
    }

    private int browserX() {
        return CHROME_PAD;
    }

    private int browserW() {
        return selectedModulePanel != null
                ? screenWidth - Panel.WIDTH - CHROME_PAD * 3
                : screenWidth - CHROME_PAD * 2;
    }

    private int browserH() {
        return Math.max(40, screenHeight - browserY() - CHROME_PAD);
    }

    private int searchX() {
        return CHROME_PAD;
    }

    private int searchY() {
        return CHROME_PAD;
    }

    private int searchW() {
        return Math.min(320, screenWidth - CHROME_PAD * 2);
    }

    public void render(RenderContext ctx, double mouseX, double mouseY) {
        // Drive animations from render so motion stays smooth at display refresh rate.
        tick(frameDeltaSeconds());
        this.textMetrics = ctx;
        this.screenWidth = ctx.screenWidth();
        this.screenHeight = ctx.screenHeight();
        this.fontHeight = ctx.fontHeight();
        Theme theme = themes.active();

        cardBrowser.setSearchQuery(searchField.query());

        switch (view) {
            case MAIN_MENU -> {
                mainMenu.renderBackground(ctx, theme, openFade);
                int px = mainMenu.panelX(screenWidth);
                int py = mainMenu.panelY(screenWidth, screenHeight, menuSlide);
                mainMenu.renderPanel(ctx, theme, px, py, adapter.playerName(),
                        PrimeDesign.VERSION, mouseX, mouseY, menuSlide);
            }
            case ONBOARDING -> renderOnboarding(ctx, theme, mouseX, mouseY);
            case SETTINGS -> {
                mainMenu.renderBackground(ctx, theme, openFade);
                settingsMenu.render(ctx, theme, themes, profiles, cloudSync, adapter, keybinds,
                        modules, screenWidth, screenHeight, mouseX, mouseY);
            }
            case COSMETICS -> {
                mainMenu.renderBackground(ctx, theme, openFade);
                cosmeticsMenu.render(ctx, theme, cosmetics, screenWidth, screenHeight, mouseX, mouseY);
            }
            case CONFIGURATIONS -> {
                mainMenu.renderBackground(ctx, theme, openFade);
                configurationsMenu.render(ctx, theme, cloudSync, profiles, screenWidth, screenHeight, mouseX, mouseY);
            }
            case FAVORITES, BROWSE -> renderModuleBrowser(ctx, theme, mouseX, mouseY);
        }
        renderEditors(ctx, theme);
        tooltips.render(ctx, theme);
    }

    private void renderModuleBrowser(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        mainMenu.renderBackground(ctx, theme, openFade);
        searchField.render(ctx, theme, searchX(), searchY(), searchW());
        cardBrowser.render(ctx, theme, browserX(), browserY(), browserW(), browserH(), mouseX, mouseY);
        if (selectedModulePanel != null) {
            selectedModulePanel.x = screenWidth - Panel.WIDTH - CHROME_PAD;
            selectedModulePanel.y = browserY();
            selectedModulePanel.render(ctx, theme, mouseX, mouseY);
            selectedModulePanel.renderDropdown(ctx, theme, mouseX, mouseY);
        }
    }

    private void renderEditors(RenderContext ctx, Theme theme) {
        if (stringEditor != null) {
            int[] pos = GuiLayout.clampPopup(editorX, editorY, 140, PrimeDesign.INPUT_HEIGHT,
                    screenWidth, screenHeight);
            editorX = pos[0];
            editorY = pos[1];
            stringEditor.render(ctx, theme, editorX, editorY, 140);
        }
        if (colorPicker != null) {
            int[] pos = GuiLayout.clampPopup(editorX, editorY,
                    ColorPickerWidget.WIDTH, ColorPickerWidget.HEIGHT, screenWidth, screenHeight);
            editorX = pos[0];
            editorY = pos[1];
            colorPicker.render(ctx, theme, editorX, editorY);
        }
    }

    private void renderOnboarding(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        mainMenu.renderBackground(ctx, theme, openFade);
        OnboardingScreen.render(ctx, theme, onboarding, screenWidth, screenHeight, menuSlide, mouseX, mouseY);
    }

    private void completeOnboarding() {
        if (onboardingCompleteHandler != null) {
            onboardingCompleteHandler.run();
        }
        view = ClickGuiView.MAIN_MENU;
    }

    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (colorPicker != null && colorPicker.mousePressed(mouseX, mouseY, editorX, editorY, button)) {
            return true;
        }
        if (stringEditor != null && stringEditor.hit(mouseX, mouseY, editorX, editorY, 140)) {
            stringEditor.setFocused(true);
            return true;
        }
        if (view == ClickGuiView.ONBOARDING) {
            if (OnboardingScreen.mousePressed(onboarding, mouseX, mouseY, screenWidth, screenHeight,
                    menuSlide, button)) {
                if (onboarding.completed()) {
                    completeOnboarding();
                }
                return true;
            }
            return true;
        }
        if (view == ClickGuiView.MAIN_MENU) {
            return handleMainMenuPress(mouseX, mouseY);
        }
        if (view == ClickGuiView.SETTINGS) {
            return settingsMenu.mousePressed(textMetrics, mouseX, mouseY, screenWidth, screenHeight, themes, keybinds,
                    adapter, modules, profiles);
        }
        if (view == ClickGuiView.COSMETICS) {
            return cosmeticsMenu.mousePressed(textMetrics, mouseX, mouseY, screenWidth, screenHeight, cosmetics);
        }
        if (view == ClickGuiView.CONFIGURATIONS) {
            return configurationsMenu.mousePressed(mouseX, mouseY, button, cloudSync, profiles,
                    screenWidth, screenHeight);
        }
        if (isModuleBrowserView()) {
            return pressModuleBrowser(mouseX, mouseY, button);
        }
        return false;
    }

    private boolean pressModuleBrowser(double mouseX, double mouseY, int button) {
        if (button == 0 && searchField.hit(mouseX, mouseY, searchX(), searchY(), searchW())) {
            searchField.setFocused(true);
            return true;
        }
        if (selectedModulePanel != null
                && selectedModulePanel.dropdownMousePressed(mouseX, mouseY)) {
            touchSelectedRecent();
            return true;
        }
        if (selectedModulePanel != null && dispatchPress(selectedModulePanel, mouseX, mouseY, button)) {
            touchSelectedRecent();
            return true;
        }
        searchField.setFocused(false);
        return cardBrowser.mousePressed(textMetrics, mouseX, mouseY,
                browserX(), browserY(), browserW(), browserH(), button);
    }

    private void touchSelectedRecent() {
        Module sel = cardBrowser.selected();
        if (sel != null) {
            recent.touch(sel.id());
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (isModuleBrowserView()) {
            return cardBrowser.mouseScrolled(verticalAmount, browserX(), browserY(), browserW(), browserH());
        }
        if (view == ClickGuiView.COSMETICS) {
            return cosmeticsMenu.scroll(verticalAmount);
        }
        if (view == ClickGuiView.SETTINGS) {
            return settingsMenu.scroll(verticalAmount);
        }
        return false;
    }

    private boolean handleMainMenuPress(double mouseX, double mouseY) {
        int btn = mainMenu.hitButton(mouseX, mouseY, screenWidth, screenHeight, menuSlide);
        if (btn < 0) {
            return false;
        }
        if (btn == 0) {
            adapter.closeCurrentScreen();
            return true;
        }
        if (btn == 3) {
            adapter.openHudEditor();
            return true;
        }
        ClickGuiView next = mainMenu.viewForButton(btn);
        if (next != null) {
            view = next;
            if (next == ClickGuiView.FAVORITES) {
                cardBrowser.showFavoritesTab();
                searchField.setFocused(true);
            } else if (next == ClickGuiView.BROWSE) {
                cardBrowser.showOverviewTab();
                searchField.setFocused(true);
            }
        }
        return true;
    }

    private void openStringEditor(StringSetting setting, int x, int y) {
        editingString = setting;
        stringEditor = new TextInputField(setting.get(), setting.name(), 48);
        stringEditor.setFocused(true);
        int[] pos = GuiLayout.clampPopup(x, y, 140, PrimeDesign.INPUT_HEIGHT, screenWidth, screenHeight);
        editorX = pos[0];
        editorY = pos[1];
        colorPicker = null;
        editingColor = null;
    }

    private void openColorEditor(ColorSetting setting, int x, int y) {
        editingColor = setting;
        colorPicker = new ColorPickerWidget();
        colorPicker.load(setting.get());
        int[] pos = GuiLayout.clampPopup(x, y, ColorPickerWidget.WIDTH, ColorPickerWidget.HEIGHT,
                screenWidth, screenHeight);
        editorX = pos[0];
        editorY = pos[1];
        stringEditor = null;
        editingString = null;
    }

    private boolean dispatchPress(Panel panel, double mouseX, double mouseY, int button) {
        Panel.Hit hit = panel.mousePressed(mouseX, mouseY, button);
        if (hit.isMiss()) {
            return false;
        }
        if (hit.isStringEdit()) {
            openStringEditor(hit.stringSetting(), (int) mouseX, (int) mouseY);
            return true;
        }
        if (hit.isColorEdit()) {
            openColorEditor(hit.colorSetting(), (int) mouseX, (int) mouseY);
            return true;
        }
        this.draggingPanel = panel;
        if (hit.isSlider()) {
            this.sliderPanel = panel;
            this.sliderSetting = hit.slider();
        }
        return true;
    }

    public void mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (colorPicker != null) {
            colorPicker.mouseDragged(mouseX, mouseY, editorX, editorY);
            if (editingColor != null) {
                editingColor.set(colorPicker.selectedArgb());
            }
            return;
        }
        if (sliderSetting != null) {
            sliderPanel.dragSlider(sliderSetting, mouseX);
            touchSelectedRecent();
            return;
        }
        if (draggingPanel != null) {
            draggingPanel.mouseDragged(mouseX, mouseY, screenWidth, screenHeight);
        }
    }

    public void mouseReleased() {
        if (draggingPanel != null) {
            draggingPanel.mouseReleased();
        }
        draggingPanel = null;
        sliderPanel = null;
        sliderSetting = null;
        if (colorPicker != null) {
            colorPicker.mouseReleased();
        }
    }

    public boolean charTyped(char character) {
        if (colorPicker != null && colorPicker.charTyped(character)) {
            if (editingColor != null) {
                editingColor.set(colorPicker.selectedArgb());
            }
            return true;
        }
        if (stringEditor != null && stringEditor.charTyped(character)) {
            if (editingString != null) {
                editingString.set(stringEditor.text());
            }
            return true;
        }
        if (view == ClickGuiView.MAIN_MENU || view == ClickGuiView.ONBOARDING) {
            return false;
        }
        if (view == ClickGuiView.SETTINGS) {
            return settingsMenu.charTyped(character);
        }
        if (view == ClickGuiView.CONFIGURATIONS || view == ClickGuiView.COSMETICS) {
            return false;
        }
        if (!isModuleBrowserView()) {
            return false;
        }
        if (character < ' ') {
            return false;
        }
        searchField.setFocused(true);
        if (searchField.charTyped(character)) {
            cardBrowser.setSearchQuery(searchField.query());
            return true;
        }
        return false;
    }

    public boolean keyPressed(int glfwKey) {
        if (colorPicker != null && colorPicker.keyPressed(glfwKey)) {
            if (editingColor != null) {
                editingColor.set(colorPicker.selectedArgb());
            }
            return true;
        }
        if (stringEditor != null) {
            boolean handled = stringEditor.keyPressed(glfwKey);
            if (handled && editingString != null && glfwKey != 256) {
                editingString.set(stringEditor.text());
            }
            // Escape closes without further navigation; Enter commits + closes.
            if (glfwKey == 256 || glfwKey == 257) {
                if (glfwKey == 257 && editingString != null) {
                    editingString.set(stringEditor.text());
                }
                closeEditors();
                return true;
            }
            // Keep focus: don't leak Backspace/arrows into module search.
            if (handled || stringEditor.focused()) {
                return true;
            }
        }
        if (view == ClickGuiView.SETTINGS && settingsMenu.capturingKey()) {
            return settingsMenu.captureKey(glfwKey, keybinds);
        }
        if (glfwKey == 256 && colorPicker != null) {
            closeEditors();
            return true;
        }
        if (glfwKey == 256 && selectedModulePanel != null && selectedModulePanel.closeDropdown()) {
            return true;
        }
        if (glfwKey == 256 && isSearching()) {
            searchField.clear();
            searchField.setFocused(false);
            cardBrowser.setSearchQuery("");
            return true;
        }
        if (glfwKey == 256 && view == ClickGuiView.ONBOARDING) {
            onboarding.skip();
            completeOnboarding();
            return true;
        }
        if (glfwKey == 256 && isModuleBrowserView() && selectedModulePanel != null) {
            selectedModulePanel = null;
            cardBrowser.selectForTests(null);
            return true;
        }
        if (glfwKey == 256 && view != ClickGuiView.MAIN_MENU) {
            closeEditors();
            view = ClickGuiView.MAIN_MENU;
            return true;
        }
        if (view == ClickGuiView.CONFIGURATIONS
                && configurationsMenu.keyPressed(glfwKey, cloudSync, profiles)) {
            return true;
        }
        if (view == ClickGuiView.SETTINGS && settingsMenu.keyPressed(glfwKey)) {
            return true;
        }
        if (view == ClickGuiView.MAIN_MENU) {
            return false;
        }
        if (isModuleBrowserView() && searchField.focused() && searchField.keyPressed(glfwKey)) {
            cardBrowser.setSearchQuery(searchField.query());
            return true;
        }
        // Backspace clears search even if focus was lost after typing.
        if (glfwKey == 259 && isModuleBrowserView() && !searchField.query().isEmpty()) {
            searchField.setFocused(true);
            searchField.keyPressed(glfwKey);
            cardBrowser.setSearchQuery(searchField.query());
            return true;
        }
        return false;
    }

    private boolean isSearching() {
        return !searchField.query().isEmpty();
    }

    private void refreshFavoritesPanelIfNeeded() {
        int revision = favorites.revision();
        if (favoritesPanel != null && revision == favoritesRevision) {
            return;
        }
        float x = favoritesPanel != null ? favoritesPanel.x : 8;
        float y = favoritesPanel != null ? favoritesPanel.y : 8;
        favoritesPanel = new Panel(PrimeLang.get("prime.gui.clickgui.favorites", "Favorites"),
                favorites.resolve(modules), favorites, x, y);
        favoritesPanel.collapsed = false;
        favoritesRevision = revision;
    }

    @Override
    public String configKey() {
        return "clickgui";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (Panel panel : panels) {
            JsonObject section = new JsonObject();
            section.addProperty("x", panel.x);
            section.addProperty("y", panel.y);
            section.addProperty("collapsed", panel.collapsed);
            json.add(panel.title(), section);
        }
        refreshFavoritesPanelIfNeeded();
        JsonObject fav = new JsonObject();
        fav.addProperty("x", favoritesPanel.x);
        fav.addProperty("y", favoritesPanel.y);
        json.add("Favorites", fav);
        json.add("recent", recent.toJson());
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        JsonObject json = element.getAsJsonObject();
        for (Panel panel : panels) {
            loadPanelSection(json, panel);
        }
        JsonElement favSection = json.get("Favorites");
        if (favSection != null && favSection.isJsonObject()) {
            JsonObject section = favSection.getAsJsonObject();
            if (section.has("x")) {
                favoritesPanel.x = section.get("x").getAsFloat();
            }
            if (section.has("y")) {
                favoritesPanel.y = section.get("y").getAsFloat();
            }
        }
        if (json.has("recent")) {
            recent.fromJson(json.get("recent"));
        }
    }

    private static void loadPanelSection(JsonObject json, Panel panel) {
        JsonElement sectionJson = json.get(panel.title());
        if (sectionJson == null || !sectionJson.isJsonObject()) {
            return;
        }
        JsonObject section = sectionJson.getAsJsonObject();
        if (section.has("x")) {
            panel.x = section.get("x").getAsFloat();
        }
        if (section.has("y")) {
            panel.y = section.get("y").getAsFloat();
        }
        if (section.has("collapsed")) {
            panel.collapsed = section.get("collapsed").getAsBoolean();
        }
    }
}
