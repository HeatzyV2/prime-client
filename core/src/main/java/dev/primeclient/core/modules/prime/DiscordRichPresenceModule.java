package dev.primeclient.core.modules.prime;

import dev.primeclient.core.account.PrimeAccountService;
import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.discord.DiscordRpcService;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.event.WorldJoinEvent;
import dev.primeclient.core.event.WorldLeaveEvent;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.IntSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.module.ModuleManager;

/**
 * Discord Rich Presence — live server IP, username, stats and Prime branding.
 *
 * <p>Application ID: {@link DiscordRpcService#APPLICATION_ID}</p>
 */
public final class DiscordRichPresenceModule extends Module {

    private final BooleanSetting showServerIp =
            addSetting(new BooleanSetting("server-ip", "Show server IP", "Display multiplayer address", true));
    private final BooleanSetting showHealth =
            addSetting(new BooleanSetting("health", "Show health", "Display HP in status line", true));
    private final BooleanSetting showPing =
            addSetting(new BooleanSetting("ping", "Show ping", "Display latency on servers", true));
    private final BooleanSetting showBiome =
            addSetting(new BooleanSetting("biome", "Show biome", "Display current biome", true));
    private final BooleanSetting showCoordinates =
            addSetting(new BooleanSetting("coords", "Show coordinates", "Display XYZ position", false));
    private final BooleanSetting showHeldItem =
            addSetting(new BooleanSetting("held-item", "Show held item", "Display item in hand", true));
    private final BooleanSetting showModuleCount =
            addSetting(new BooleanSetting("modules", "Show modules", "Display enabled module count", true));
    private final BooleanSetting showSessionTime =
            addSetting(new BooleanSetting("session", "Session timer", "Show elapsed session time", true));
    private final BooleanSetting showAccountTier =
            addSetting(new BooleanSetting("tier", "Show Prime tier", "Display account tier", true));
    private final BooleanSetting showFps =
            addSetting(new BooleanSetting("fps", "Show FPS", "Display current FPS", false));
    private final IntSetting updateInterval =
            addSetting(new IntSetting("interval", "Update interval", "Ticks between RPC refresh", 40, 20, 200));

    private final DiscordRpcService discord;
    private final MinecraftAdapter adapter;
    private final ModuleManager modules;
    private final PrimeAccountService account;

    public DiscordRichPresenceModule(DiscordRpcService discord, MinecraftAdapter adapter,
                                     ModuleManager modules, PrimeAccountService account) {
        super("discord-rpc", "Discord RPC", "Rich Presence with server, stats and Prime branding",
                ModuleCategory.PRIME);
        this.discord = discord;
        this.adapter = adapter;
        this.modules = modules;
        this.account = account;

        listen(ClientTickEvent.class, event -> onTick());
        listen(WorldJoinEvent.class, event -> {
            syncSettings();
            discord.onWorldJoin();
            discord.forceUpdate(adapter, modules, account);
        });
        listen(WorldLeaveEvent.class, event -> discord.onWorldLeave());
    }

    @Override
    protected void onEnable() {
        syncSettings();
        discord.start();
        discord.forceUpdate(adapter, modules, account);
    }

    @Override
    protected void onDisable() {
        discord.stop();
    }

    private void onTick() {
        syncSettings();
        discord.tick(adapter, modules, account);
    }

    private void syncSettings() {
        var s = discord.settings();
        s.setShowServerIp(showServerIp.get());
        s.setShowHealth(showHealth.get());
        s.setShowPing(showPing.get());
        s.setShowBiome(showBiome.get());
        s.setShowCoordinates(showCoordinates.get());
        s.setShowHeldItem(showHeldItem.get());
        s.setShowModuleCount(showModuleCount.get());
        s.setShowSessionTime(showSessionTime.get());
        s.setShowAccountTier(showAccountTier.get());
        s.setShowFps(showFps.get());
        s.setUpdateIntervalTicks(updateInterval.get());
    }
}
