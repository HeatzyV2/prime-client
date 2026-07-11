package dev.primeclient.core;

import dev.primeclient.core.account.PrimeAccountService;
import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.cloud.CloudSyncManager;
import dev.primeclient.core.cloud.LocalCloudClient;
import dev.primeclient.core.cloud.RemoteCloudClient;
import dev.primeclient.core.config.ConfigManager;
import dev.primeclient.core.cosmetics.CosmeticManager;
import dev.primeclient.core.crosshair.CrosshairConfig;
import dev.primeclient.core.crosshair.CrosshairPresetStore;
import dev.primeclient.core.crosshair.CrosshairProfileManager;
import dev.primeclient.core.bootstrap.FirstRunConfigurator;
import dev.primeclient.core.bootstrap.OnboardingFlow;
import dev.primeclient.core.discord.DiscordRpcService;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.event.EventBus;
import dev.primeclient.core.event.WorldJoinEvent;
import dev.primeclient.core.event.WorldLeaveEvent;
import dev.primeclient.core.module.ModuleToggleEvent;
import dev.primeclient.core.gui.FavoritesManager;
import dev.primeclient.core.gui.TooltipRenderer;
import dev.primeclient.core.gui.clickgui.ClickGui;
import dev.primeclient.core.gui.menu.LoadingOverlay;
import dev.primeclient.core.gui.menu.OnboardingManager;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.editor.HudEditor;
import dev.primeclient.core.hud.elements.NotificationsElement;
import dev.primeclient.core.hud.elements.WatermarkElement;
import dev.primeclient.core.keybind.KeybindManager;
import dev.primeclient.core.module.ModuleManager;
import dev.primeclient.core.notification.NotificationManager;
import dev.primeclient.core.notification.NotificationPreferences;
import dev.primeclient.core.profile.ProfileManager;
import dev.primeclient.core.replay.ReplaySession;
import dev.primeclient.core.replay.ReplayStorage;
import dev.primeclient.core.clip.ClipRecorder;
import dev.primeclient.core.clip.ClipStorage;
import dev.primeclient.core.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/** Prime Client entry point and service root. */
public final class PrimeClient {

    public static final String MOD_ID = "primeclient";
    public static final String NAME = "Prime Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static volatile PrimeClient instance;

    private final MinecraftAdapter adapter;
    private final EventBus eventBus;
    private final ConfigManager configManager;
    private final KeybindManager keybinds;
    private final ModuleManager modules;
    private final ThemeManager themes;
    private final NotificationManager notifications;
    private final NotificationPreferences notificationPrefs;
    private final HudManager hud;
    private final HudEditor hudEditor;
    private final FavoritesManager favorites;
    private final ClickGui clickGui;
    private final ProfileManager profiles;
    private final CrosshairConfig crosshairConfig;
    private final CrosshairPresetStore crosshairPresets;
    private final CrosshairProfileManager crosshairProfiles;
    private final CosmeticManager cosmetics;
    private final CloudSyncManager cloudSync;
    private final PrimeAccountService account;
    private final OnboardingManager onboarding;
    private final LoadingOverlay loadingOverlay;
    private final ReplaySession replaySession;
    private final ReplayStorage replayStorage;
    private final ClipStorage clipStorage;
    private final ClipRecorder clipRecorder;
    private final TooltipRenderer tooltips;
    private final DiscordRpcService discordRpc;

    private boolean debutSession;
    private int debutTicks;
    private boolean debutMenuOpened;

    private PrimeClient(MinecraftAdapter adapter) {
        this.adapter = adapter;
        this.eventBus = new EventBus();
        this.configManager = new ConfigManager();
        this.keybinds = new KeybindManager();
        this.modules = new ModuleManager(eventBus, keybinds);
        this.themes = new ThemeManager();
        this.notifications = new NotificationManager();
        this.notificationPrefs = new NotificationPreferences();
        this.hud = new HudManager();
        this.hudEditor = new HudEditor(hud, themes);
        this.favorites = new FavoritesManager();
        this.crosshairConfig = new CrosshairConfig();
        this.crosshairPresets = new CrosshairPresetStore();
        this.crosshairProfiles = new CrosshairProfileManager(crosshairConfig);
        this.cosmetics = new CosmeticManager();
        this.onboarding = new OnboardingManager();
        this.loadingOverlay = new LoadingOverlay();
        this.replaySession = new ReplaySession();
        this.tooltips = new TooltipRenderer();
        this.account = new PrimeAccountService();
        this.discordRpc = new DiscordRpcService();

        Path modRoot = adapter.configDirectory().resolve(MOD_ID);
        LocalCloudClient localCloud = new LocalCloudClient(modRoot.resolve("cloud"));
        RemoteCloudClient remoteCloud = new RemoteCloudClient(localCloud, account);
        this.cloudSync = new CloudSyncManager(remoteCloud, configManager, notifications);
        this.replayStorage = new ReplayStorage(modRoot);
        this.clipStorage = new ClipStorage(modRoot);
        this.clipRecorder = new ClipRecorder(clipStorage, notifications);
        this.profiles = new ProfileManager(configManager, modRoot);
        this.clickGui = new ClickGui(modules, themes, favorites, adapter, onboarding,
                cloudSync, cosmetics, profiles, tooltips);
        this.clickGui.setOnboardingCompleteHandler(() -> OnboardingFlow.applyChoices(this));

        configManager.register(keybinds);
        configManager.register(modules);
        configManager.register(themes);
        configManager.register(hud);
        configManager.register(favorites);
        configManager.register(clickGui);
        configManager.register(crosshairConfig);
        configManager.register(crosshairPresets);
        configManager.register(crosshairProfiles);
        configManager.register(cosmetics);
        configManager.register(onboarding);
        configManager.register(account);
        configManager.register(notificationPrefs);
        configManager.register(discordRpc.settings());

        hud.register(new WatermarkElement(themes, adapter.minecraftVersion()));
        hud.register(new NotificationsElement(notifications, themes, notificationPrefs));

        if (account.username().isBlank() && adapter.hasPlayer()) {
            account.login(adapter.playerName());
        }

        keybinds.register(new dev.primeclient.core.keybind.Keybind("click-gui", "ClickGUI", "Prime", 344)
                .onPress(adapter::openClickGui));
        keybinds.register(new dev.primeclient.core.keybind.Keybind("hud-editor", "HUD Editor", "Prime", 72)
                .onPress(adapter::openHudEditor));
    }

    public static synchronized void bootstrap(MinecraftAdapter adapter) {
        if (instance != null) {
            throw new IllegalStateException(NAME + " is already bootstrapped");
        }
        PrimeClient client = new PrimeClient(adapter);
        instance = client;
        client.loadingOverlay.setStage("Loading Core...", 0.2f);
        Modules.registerBuiltins(client);
        client.wireGlobalListeners();
        client.loadingOverlay.setStage("Loading Modules...", 0.55f);
        boolean freshInstall = client.profiles.loadInitial();
        client.debutSession = freshInstall;
        if (freshInstall) {
            FirstRunConfigurator.applyStarter(client);
        }
        client.loadingOverlay.setStage("Prime Client prêt", 1f);
        LOGGER.info("{} v{} bootstrapped (Minecraft {}, {} modules, profile '{}'{})",
                NAME, dev.primeclient.core.design.PrimeDesign.VERSION,
                adapter.minecraftVersion(), client.modules.all().size(), client.profiles.activeProfile(),
                freshInstall ? ", first launch" : "");
    }

    private void wireGlobalListeners() {
        eventBus.subscribe(ModuleToggleEvent.class, event -> {
            if (!notificationPrefs.moduleToggleNotifs()) {
                return;
            }
            String verb = event.enabled() ? "Enabled" : "Disabled";
            if (event.enabled()) {
                notifications.success(event.module().name(), verb);
            } else {
                notifications.info(event.module().name(), verb);
            }
        });
    }

    public void tick() {
        loadingOverlay.tick(1f / 20f);
        if (debutSession) {
            debutTicks++;
            if (debutTicks == 60) {
                loadingOverlay.hide();
            }
            if (debutTicks == 40 && !onboarding.completed() && !adapter.isScreenOpen()) {
                adapter.openClickGui();
                debutMenuOpened = true;
            }
            if (debutTicks > 200) {
                debutSession = false;
            }
        }
        tooltips.tick(50);
        if (adapter.isScreenOpen()) {
            keybinds.releaseAll();
        } else {
            keybinds.poll(adapter::isKeyDown);
        }
        eventBus.post(ClientTickEvent.INSTANCE);
    }

    public void onWorldJoin() {
        if (!account.loggedIn() && adapter.hasPlayer()) {
            account.login(adapter.playerName());
        }
        crosshairProfiles.applyForServer(adapter.serverAddress());
        eventBus.post(WorldJoinEvent.INSTANCE);
    }

    public void onWorldLeave() {
        crosshairProfiles.saveCurrentForServer(adapter.serverAddress());
        if (cloudSync.autoSync() && account.loggedIn()) {
            cloudSync.uploadNow(profiles.activeProfile());
        }
        eventBus.post(WorldLeaveEvent.INSTANCE);
    }

    public void shutdown() {
        discordRpc.shutdown();
        profiles.saveActive();
        LOGGER.info("{} shut down, config saved", NAME);
    }

    public static PrimeClient get() {
        PrimeClient current = instance;
        if (current == null) {
            throw new IllegalStateException(NAME + " is not bootstrapped yet");
        }
        return current;
    }

    public MinecraftAdapter adapter() { return adapter; }
    public EventBus events() { return eventBus; }
    public ConfigManager config() { return configManager; }
    public KeybindManager keybinds() { return keybinds; }
    public ModuleManager modules() { return modules; }
    public ThemeManager themes() { return themes; }
    public NotificationManager notifications() { return notifications; }
    public NotificationPreferences notificationPrefs() { return notificationPrefs; }
    public HudManager hud() { return hud; }
    public HudEditor hudEditor() { return hudEditor; }
    public ClickGui clickGui() { return clickGui; }
    public FavoritesManager favorites() { return favorites; }
    public ProfileManager profiles() { return profiles; }
    public CrosshairConfig crosshairConfig() { return crosshairConfig; }
    public CrosshairPresetStore crosshairPresets() { return crosshairPresets; }
    public CrosshairProfileManager crosshairProfiles() { return crosshairProfiles; }
    public CosmeticManager cosmetics() { return cosmetics; }
    public CloudSyncManager cloudSync() { return cloudSync; }
    public PrimeAccountService account() { return account; }
    public OnboardingManager onboarding() { return onboarding; }
    public LoadingOverlay loadingOverlay() { return loadingOverlay; }
    public ReplaySession replaySession() { return replaySession; }
    public ReplayStorage replayStorage() { return replayStorage; }
    public ClipStorage clipStorage() { return clipStorage; }
    public ClipRecorder clipRecorder() { return clipRecorder; }
    public TooltipRenderer tooltips() { return tooltips; }
    public DiscordRpcService discordRpc() { return discordRpc; }
}
