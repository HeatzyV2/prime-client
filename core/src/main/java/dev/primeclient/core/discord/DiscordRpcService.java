package dev.primeclient.core.discord;

import dev.primeclient.core.account.PrimeAccountService;
import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.i18n.PrimeLang;
import dev.primeclient.core.discord.ipc.DiscordIpcClient;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds and publishes Discord Rich Presence from live game state. */
public final class DiscordRpcService {

    public static final String APPLICATION_ID = "1525574680994648174";

    private final DiscordIpcClient ipc = new DiscordIpcClient(APPLICATION_ID);
    private final DiscordPresenceSettings settings = new DiscordPresenceSettings();

    private boolean running;
    private String lastFingerprint = "";
    private int tickCounter;
    private long menuSinceMillis = System.currentTimeMillis();
    private long worldSinceMillis;

    public DiscordPresenceSettings settings() {
        return settings;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        lastFingerprint = "";
        ipc.connectAsync();
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        lastFingerprint = "";
        ipc.clearAsync();
    }

    public void shutdown() {
        stop();
        ipc.shutdown();
    }

    public void onWorldJoin() {
        worldSinceMillis = System.currentTimeMillis();
        lastFingerprint = "";
    }

    public void onWorldLeave() {
        lastFingerprint = "";
        menuSinceMillis = System.currentTimeMillis();
    }

    public void tick(MinecraftAdapter adapter, ModuleManager modules, PrimeAccountService account) {
        if (!running) {
            return;
        }
        tickCounter++;
        if (tickCounter < settings.updateIntervalTicks()) {
            return;
        }
        tickCounter = 0;
        publishIfChanged(buildSnapshot(adapter, modules, account));
    }

    public void forceUpdate(MinecraftAdapter adapter, ModuleManager modules, PrimeAccountService account) {
        if (!running) {
            return;
        }
        if (adapter == null) {
            ipc.connectAsync();
            return;
        }
        publishIfChanged(buildSnapshot(adapter, modules, account));
    }

    private void publishIfChanged(DiscordPresenceSnapshot snapshot) {
        String fingerprint = snapshot.fingerprint();
        if (fingerprint.equals(lastFingerprint)) {
            return;
        }
        lastFingerprint = fingerprint;
        ipc.updateAsync(snapshot);
    }

    DiscordPresenceSnapshot buildSnapshot(MinecraftAdapter adapter, ModuleManager modules,
                                          PrimeAccountService account) {
        String player = displayName(adapter, account);
        String mcVersion = adapter.minecraftVersion();
        String primeVersion = PrimeDesign.VERSION;

        if (!adapter.isInGame() || !adapter.hasPlayer()) {
            return menuPresence(player, mcVersion, primeVersion, adapter);
        }

        String server = adapter.serverAddress();
        boolean singleplayer = server == null || server.isBlank()
                || "Singleplayer".equalsIgnoreCase(server);

        String details = singleplayer
                ? PrimeLang.get("prime.discord.singleplayer", "Singleplayer")
                : PrimeLang.get("prime.discord.playing_on", "Playing on %s",
                        settings.showServerIp() ? server : maskServer(server));

        StringBuilder state = new StringBuilder();
        state.append(player);
        if (settings.showAccountTier() && account.loggedIn()) {
            state.append(" • ").append(account.tier().name());
        }
        if (settings.showPing() && !singleplayer) {
            int ping = adapter.ping();
            if (ping > 0) {
                state.append(" • ").append(ping).append("ms");
            }
        }
        if (settings.showHealth()) {
            state.append(" • ♥ ")
                    .append(Math.round(adapter.playerHealth()))
                    .append("/")
                    .append(Math.round(adapter.playerMaxHealth()));
        }
        if (settings.showBiome()) {
            String biome = adapter.biomeName();
            if (!biome.isBlank()) {
                state.append(" • ").append(biome);
            }
        }
        if (settings.showCoordinates()) {
            state.append(" • ")
                    .append(formatCoord(adapter.playerX()))
                    .append(", ")
                    .append(formatCoord(adapter.playerY()))
                    .append(", ")
                    .append(formatCoord(adapter.playerZ()));
        }
        if (settings.showHeldItem()) {
            String item = adapter.heldItemName();
            if (!item.isBlank()) {
                state.append(" • ").append(item);
            }
        }
        if (settings.showModuleCount()) {
            int enabled = countEnabled(modules);
            state.append(" • ").append(PrimeLang.get("prime.discord.modules_count", "%1$d/%2$d modules",
                    enabled, modules.all().size()));
        }
        if (settings.showFps()) {
            state.append(" • ").append(PrimeLang.get("prime.discord.fps", "%d FPS", adapter.fps()));
        }

        String smallKey = "prime_logo";
        String smallText = singleplayer
                ? PrimeLang.get("prime.discord.singleplayer", "Singleplayer")
                : server;

        Long start = settings.showSessionTime()
                ? worldSinceMillis / 1000L
                : null;

        List<DiscordPresenceSnapshot.Button> buttons = buildButtons(server, singleplayer);

        return new DiscordPresenceSnapshot(
                details,
                state.toString(),
                "prime_logo",
                PrimeLang.get("prime.discord.client_version", "Prime Client v%s", primeVersion),
                smallKey,
                smallText,
                start,
                buttons
        );
    }

    private DiscordPresenceSnapshot menuPresence(String player, String mcVersion, String primeVersion,
                                                   MinecraftAdapter adapter) {
        String details = adapter.isScreenOpen()
                ? PrimeLang.get("prime.discord.browsing_menus", "Browsing menus")
                : PrimeLang.get("prime.discord.main_menu", "In Main Menu");
        String state = PrimeLang.get("prime.discord.state_menu", "%1$s · Minecraft %2$s · Prime v%3$s",
                player, mcVersion, primeVersion);
        if (settings.showFps()) {
            state += " • " + PrimeLang.get("prime.discord.fps", "%d FPS", adapter.fps());
        }
        Long start = settings.showSessionTime() ? menuSinceMillis / 1000L : null;
        List<DiscordPresenceSnapshot.Button> buttons = List.of(
                new DiscordPresenceSnapshot.Button(
                        PrimeLang.get("prime.discord.button.client", "Prime Client"), appUrl()),
                new DiscordPresenceSnapshot.Button(
                        PrimeLang.get("prime.discord.button.discord_app", "Discord App"), discordAppUrl())
        );
        return new DiscordPresenceSnapshot(
                details,
                state,
                "prime_logo",
                PrimeLang.get("prime.discord.client_version", "Prime Client v%s", primeVersion),
                "prime_logo",
                PrimeLang.get("prime.discord.main_menu_small", "Main Menu"),
                start,
                buttons
        );
    }

    private List<DiscordPresenceSnapshot.Button> buildButtons(String server, boolean singleplayer) {
        List<DiscordPresenceSnapshot.Button> buttons = new ArrayList<>();
        buttons.add(new DiscordPresenceSnapshot.Button(
                PrimeLang.get("prime.discord.button.client", "Prime Client"), appUrl()));
        if (!singleplayer && settings.showServerIp() && server != null && !server.isBlank()) {
            buttons.add(new DiscordPresenceSnapshot.Button(
                    PrimeLang.get("prime.discord.button.server_status", "Server Status"), serverStatusUrl(server)));
        } else {
            buttons.add(new DiscordPresenceSnapshot.Button(
                    PrimeLang.get("prime.discord.button.discord_app", "Discord App"), discordAppUrl()));
        }
        return buttons.size() > 2 ? buttons.subList(0, 2) : buttons;
    }

    private static String displayName(MinecraftAdapter adapter, PrimeAccountService account) {
        if (account.loggedIn() && !account.username().isBlank()) {
            return account.username();
        }
        String name = adapter.playerName();
        return name.isBlank() ? PrimeLang.get("prime.discord.player_fallback", "Player") : name;
    }

    private static int countEnabled(ModuleManager modules) {
        int count = 0;
        for (Module module : modules.all()) {
            if (module.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    private static String formatCoord(double value) {
        return Integer.toString((int) Math.floor(value));
    }

    private static String maskServer(String server) {
        int colon = server.indexOf(':');
        if (colon <= 0) {
            return "••••••••";
        }
        return server.substring(0, Math.min(4, colon)) + "•••";
    }

    private static String appUrl() {
        return "https://discord.com/applications/" + APPLICATION_ID;
    }

    private static String discordAppUrl() {
        return "https://discord.com/app";
    }

    private static String serverStatusUrl(String server) {
        String encoded = URLEncoder.encode(server, StandardCharsets.UTF_8);
        return "https://mcsrvstat.us/server/" + encoded;
    }
}
