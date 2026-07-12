package dev.primeclient.core;

import dev.primeclient.core.modules.creator.CameraZoomModule;
import dev.primeclient.core.modules.creator.CinematicCameraModule;
import dev.primeclient.core.modules.creator.ClipToolsModule;
import dev.primeclient.core.modules.creator.ReplayToolsModule;
import dev.primeclient.core.modules.creator.ScreenshotModeModule;
import dev.primeclient.core.modules.creator.StreamerModeModule;
import dev.primeclient.core.modules.performance.AnimationOptimizerModule;
import dev.primeclient.core.modules.performance.ChunkOptimizerModule;
import dev.primeclient.core.modules.performance.DynamicFpsModule;
import dev.primeclient.core.modules.performance.EntityCullingModule;
import dev.primeclient.core.modules.performance.FastLoadingModule;
import dev.primeclient.core.modules.performance.FpsBoosterModule;
import dev.primeclient.core.modules.performance.MemoryMonitorModule;
import dev.primeclient.core.modules.performance.ParticleOptimizerModule;
import dev.primeclient.core.modules.performance.PerformanceProfilesModule;
import dev.primeclient.core.modules.performance.RamCleanerModule;
import dev.primeclient.core.modules.prime.DiscordRichPresenceModule;
import dev.primeclient.core.modules.prime.PrimeAccountModule;
import dev.primeclient.core.modules.prime.PrimeConfigCloudModule;
import dev.primeclient.core.modules.prime.PrimeCosmeticsModule;
import dev.primeclient.core.modules.prime.PrimeProfilesModule;
import dev.primeclient.core.modules.prime.PrimeSettingsManagerModule;
import dev.primeclient.core.modules.pvp.ArmorHudModule;
import dev.primeclient.core.modules.pvp.ComboCounterModule;
import dev.primeclient.core.modules.pvp.CoordinatesModule;
import dev.primeclient.core.modules.pvp.CpsCounterModule;
import dev.primeclient.core.modules.pvp.CrosshairEditorModule;
import dev.primeclient.core.modules.pvp.DamageIndicatorModule;
import dev.primeclient.core.modules.pvp.DirectionHudModule;
import dev.primeclient.core.modules.pvp.FpsCounterModule;
import dev.primeclient.core.modules.pvp.HitColorModule;
import dev.primeclient.core.modules.pvp.HitParticlesModule;
import dev.primeclient.core.modules.pvp.ItemCooldownModule;
import dev.primeclient.core.modules.pvp.KeystrokesModule;
import dev.primeclient.core.modules.pvp.PingDisplayModule;
import dev.primeclient.core.modules.pvp.PotionHudModule;
import dev.primeclient.core.modules.pvp.TargetHudModule;
import dev.primeclient.core.modules.qol.AutoGgModule;
import dev.primeclient.core.modules.qol.AutoRespawnModule;
import dev.primeclient.core.modules.qol.BetterChatModule;
import dev.primeclient.core.modules.qol.BetterTooltipsModule;
import dev.primeclient.core.modules.qol.ChatFilterModule;
import dev.primeclient.core.modules.qol.ChatTimestampModule;
import dev.primeclient.core.modules.qol.DeathWaypointModule;
import dev.primeclient.core.modules.qol.FullbrightModule;
import dev.primeclient.core.modules.qol.InventorySearchModule;
import dev.primeclient.core.modules.qol.ItemCounterModule;
import dev.primeclient.core.modules.qol.ServerSwitcherModule;
import dev.primeclient.core.modules.qol.ShulkerPreviewModule;
import dev.primeclient.core.modules.qol.ToggleSneakModule;
import dev.primeclient.core.modules.qol.ToggleSprintModule;
import dev.primeclient.core.modules.qol.WaypointsModule;
import dev.primeclient.core.modules.qol.ZoomModule;

/**
 * Single registration point of every built-in Prime module.
 *
 * <p>Explicit list, no classpath scanning: registration order is the ClickGUI
 * display order, construction is reflection-free, and a module that fails to
 * compile fails the build instead of silently disappearing.</p>
 */
final class Modules {

    private Modules() {
    }

    static void registerBuiltins(PrimeClient client) {
        var modules = client.modules();
        var hud = client.hud();
        var themes = client.themes();
        var adapter = client.adapter();

        // PvP (15)
        modules.register(new KeystrokesModule(hud, themes, adapter));
        modules.register(new CpsCounterModule(hud, themes, adapter));
        modules.register(new FpsCounterModule(hud, themes, adapter));
        modules.register(new PingDisplayModule(hud, themes, adapter));
        modules.register(new ComboCounterModule(hud, themes, adapter));
        modules.register(new TargetHudModule(hud, themes, adapter));
        modules.register(new ArmorHudModule(hud, themes, adapter));
        modules.register(new PotionHudModule(hud, themes, adapter));
        modules.register(new ItemCooldownModule(hud, themes, adapter));
        modules.register(new CrosshairEditorModule(hud, themes, adapter, client.crosshairConfig(),
                client.crosshairPresets(), client.crosshairProfiles()));
        modules.register(new HitColorModule());
        modules.register(new HitParticlesModule(adapter));
        modules.register(new DamageIndicatorModule(hud, themes, adapter));
        modules.register(new DirectionHudModule(hud, themes, adapter));
        modules.register(new CoordinatesModule(hud, themes, adapter));

        // Performance (10)
        modules.register(new FpsBoosterModule(adapter));
        modules.register(new EntityCullingModule(adapter));
        modules.register(new ParticleOptimizerModule(adapter));
        modules.register(new MemoryMonitorModule(hud, themes, adapter));
        modules.register(new RamCleanerModule(adapter));
        modules.register(new DynamicFpsModule(adapter));
        modules.register(new ChunkOptimizerModule(adapter));
        modules.register(new AnimationOptimizerModule(adapter));
        modules.register(new FastLoadingModule(adapter));
        modules.register(new PerformanceProfilesModule(adapter));

        // QoL (15)
        modules.register(new ZoomModule(adapter));
        modules.register(new FullbrightModule(adapter));
        modules.register(new ToggleSprintModule(adapter));
        modules.register(new ToggleSneakModule(adapter));
        modules.register(new AutoRespawnModule(adapter));
        modules.register(new AutoGgModule(adapter));
        modules.register(new ChatTimestampModule());
        modules.register(new BetterChatModule());
        modules.register(new ChatFilterModule());
        modules.register(new WaypointsModule(hud, themes, adapter));
        modules.register(new DeathWaypointModule(hud, themes, adapter));
        modules.register(new ItemCounterModule(hud, themes, adapter));
        modules.register(new ShulkerPreviewModule(hud, themes, adapter));
        modules.register(new BetterTooltipsModule(hud, themes, adapter));
        modules.register(new InventorySearchModule(hud, themes, adapter));
        modules.register(new ServerSwitcherModule(hud, themes, adapter));

        // Creator (6)
        modules.register(new CinematicCameraModule(hud, themes, adapter));
        modules.register(new ScreenshotModeModule(adapter));
        modules.register(new StreamerModeModule(hud, adapter));
        modules.register(new CameraZoomModule(adapter));
        modules.register(new ReplayToolsModule(hud, themes, adapter, client.replaySession(), client.replayStorage()));
        modules.register(new ClipToolsModule(hud, themes, adapter, client.clipRecorder(), client.keybinds()));

        // Prime (6)
        modules.register(new PrimeProfilesModule(client.profiles()));
        modules.register(new PrimeConfigCloudModule(client.cloudSync(), client.profiles()));
        modules.register(new PrimeCosmeticsModule(client.cosmetics()));
        modules.register(new PrimeAccountModule(hud, themes, adapter, client.account()));
        modules.register(new DiscordRichPresenceModule(client.discordRpc(), adapter, modules, client.account()));
        modules.register(new PrimeSettingsManagerModule(modules, adapter));
    }
}
