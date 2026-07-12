package dev.primeclient.core;

import dev.primeclient.core.modules.creator.CameraZoomModule;
import dev.primeclient.core.modules.creator.CinematicCameraModule;
import dev.primeclient.core.modules.creator.CinematicGridModule;
import dev.primeclient.core.modules.creator.ClipBookmarkModule;
import dev.primeclient.core.modules.creator.ClipToolsModule;
import dev.primeclient.core.modules.creator.ReplayToolsModule;
import dev.primeclient.core.modules.creator.ScreenshotModeModule;
import dev.primeclient.core.modules.creator.StreamSafeHudModule;
import dev.primeclient.core.modules.creator.StreamerModeModule;
import dev.primeclient.core.modules.performance.AdaptiveFpsModule;
import dev.primeclient.core.modules.performance.AnimationOptimizerModule;
import dev.primeclient.core.modules.performance.ChunkOptimizerModule;
import dev.primeclient.core.modules.performance.DynamicFpsModule;
import dev.primeclient.core.modules.performance.EntityCullingModule;
import dev.primeclient.core.modules.performance.FastLoadingModule;
import dev.primeclient.core.modules.performance.FpsBoosterModule;
import dev.primeclient.core.modules.performance.MemoryMonitorModule;
import dev.primeclient.core.modules.performance.MemorySpikeAlertModule;
import dev.primeclient.core.modules.performance.ParticleOptimizerModule;
import dev.primeclient.core.modules.performance.PerformanceProfilesModule;
import dev.primeclient.core.modules.performance.RamCleanerModule;
import dev.primeclient.core.modules.prime.DiscordRichPresenceModule;
import dev.primeclient.core.modules.prime.GameplayDnaModule;
import dev.primeclient.core.modules.prime.ModuleBundlesModule;
import dev.primeclient.core.modules.prime.PrimeAccountModule;
import dev.primeclient.core.modules.prime.PrimeConfigCloudModule;
import dev.primeclient.core.modules.prime.PrimeCosmeticsModule;
import dev.primeclient.core.modules.prime.PrimeProfilesModule;
import dev.primeclient.core.modules.prime.PrimeSettingsManagerModule;
import dev.primeclient.core.modules.prime.VoiceChatModule;
import dev.primeclient.core.modules.prime.ServerNotesModule;
import dev.primeclient.core.modules.prime.SmartProfileModule;
import dev.primeclient.core.modules.pvp.ArmorHudModule;
import dev.primeclient.core.modules.pvp.ChorusCooldownModule;
import dev.primeclient.core.modules.pvp.ComboCounterModule;
import dev.primeclient.core.modules.pvp.ComboTimerModule;
import dev.primeclient.core.modules.pvp.CoordinatesModule;
import dev.primeclient.core.modules.pvp.CpsCounterModule;
import dev.primeclient.core.modules.pvp.CpvpSupplyModule;
import dev.primeclient.core.modules.pvp.CritIndicatorModule;
import dev.primeclient.core.modules.pvp.CrosshairEditorModule;
import dev.primeclient.core.modules.pvp.CrystalSupplyModule;
import dev.primeclient.core.modules.pvp.DamageIndicatorModule;
import dev.primeclient.core.modules.pvp.DirectionHudModule;
import dev.primeclient.core.modules.pvp.DuelTimerModule;
import dev.primeclient.core.modules.pvp.ElytraStatusModule;
import dev.primeclient.core.modules.pvp.FoodLevelModule;
import dev.primeclient.core.modules.pvp.FpsCounterModule;
import dev.primeclient.core.modules.pvp.GappleCooldownModule;
import dev.primeclient.core.modules.pvp.HealthAlertModule;
import dev.primeclient.core.modules.pvp.HitColorModule;
import dev.primeclient.core.modules.pvp.HitParticlesModule;
import dev.primeclient.core.modules.pvp.ItemCooldownModule;
import dev.primeclient.core.modules.pvp.KeystrokesModule;
import dev.primeclient.core.modules.pvp.KnockbackIndicatorModule;
import dev.primeclient.core.modules.pvp.MaceSmashModule;
import dev.primeclient.core.modules.pvp.ObsidianSupplyModule;
import dev.primeclient.core.modules.pvp.OffhandHudModule;
import dev.primeclient.core.modules.pvp.PearlCooldownModule;
import dev.primeclient.core.modules.pvp.PearlLandingMarkerModule;
import dev.primeclient.core.modules.pvp.PingDisplayModule;
import dev.primeclient.core.modules.pvp.PotionHudModule;
import dev.primeclient.core.modules.pvp.ReachHudModule;
import dev.primeclient.core.modules.pvp.ShieldBreakAlertModule;
import dev.primeclient.core.modules.pvp.ShieldDurabilityModule;
import dev.primeclient.core.modules.pvp.ShieldStatusModule;
import dev.primeclient.core.modules.pvp.StreakCounterModule;
import dev.primeclient.core.modules.pvp.TargetHudModule;
import dev.primeclient.core.modules.pvp.TotemAlertModule;
import dev.primeclient.core.modules.pvp.TotemCounterModule;
import dev.primeclient.core.modules.pvp.WindChargeCooldownModule;
import dev.primeclient.core.modules.qol.AutoGgModule;
import dev.primeclient.core.modules.qol.AutoRespawnModule;
import dev.primeclient.core.modules.qol.BetterChatModule;
import dev.primeclient.core.modules.qol.BetterTooltipsModule;
import dev.primeclient.core.modules.qol.ChatFilterModule;
import dev.primeclient.core.modules.qol.ChatTimestampModule;
import dev.primeclient.core.modules.qol.DeathReplayModule;
import dev.primeclient.core.modules.qol.DeathWaypointModule;
import dev.primeclient.core.modules.qol.FullbrightModule;
import dev.primeclient.core.modules.qol.InventorySearchModule;
import dev.primeclient.core.modules.qol.ItemCounterModule;
import dev.primeclient.core.modules.qol.MentionHighlightModule;
import dev.primeclient.core.modules.qol.ServerSwitcherModule;
import dev.primeclient.core.modules.qol.SessionRecapModule;
import dev.primeclient.core.modules.qol.ShulkerPreviewModule;
import dev.primeclient.core.modules.qol.ToggleSneakModule;
import dev.primeclient.core.modules.qol.ToggleSprintModule;
import dev.primeclient.core.modules.qol.WaypointsModule;
import dev.primeclient.core.modules.qol.ZoomModule;
import dev.primeclient.core.modules.survival.BaseRadiusModule;
import dev.primeclient.core.modules.survival.BedReminderModule;
import dev.primeclient.core.modules.survival.CropGrowthHudModule;
import dev.primeclient.core.modules.survival.DayTimeModule;
import dev.primeclient.core.modules.survival.DeathCounterModule;
import dev.primeclient.core.modules.survival.DepthHudModule;
import dev.primeclient.core.modules.survival.ElytraFlightHudModule;
import dev.primeclient.core.modules.survival.FriendDeathPingModule;
import dev.primeclient.core.modules.survival.LightLevelModule;
import dev.primeclient.core.modules.survival.MobSpawnSafeModule;
import dev.primeclient.core.modules.survival.RaidAlertModule;
import dev.primeclient.core.modules.survival.SaturationHudModule;
import dev.primeclient.core.modules.survival.SpawnDistanceModule;
import dev.primeclient.core.modules.survival.StructureLogModule;
import dev.primeclient.core.modules.survival.TeamTagHudModule;
import dev.primeclient.core.modules.survival.ToolDurabilityModule;
import dev.primeclient.core.modules.survival.VillagerTradeLogModule;
import dev.primeclient.core.modules.survival.WeatherHudModule;
import dev.primeclient.core.modules.smp.AfkAlertModule;
import dev.primeclient.core.modules.smp.BiomeCoordsModule;
import dev.primeclient.core.modules.smp.ChunkCoordsModule;
import dev.primeclient.core.modules.smp.DeathCostModule;
import dev.primeclient.core.modules.smp.NetherLinkModule;
import dev.primeclient.core.modules.smp.RepairAlertModule;
import dev.primeclient.core.modules.smp.ServerSessionModule;
import dev.primeclient.core.modules.smp.ShopWaypointModule;
import dev.primeclient.core.modules.smp.SpawnCompassModule;
import dev.primeclient.core.modules.smp.TravelEtaModule;

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

        // PvP (38)
        modules.register(new KeystrokesModule(hud, themes, adapter));
        modules.register(new CpsCounterModule(hud, themes, adapter));
        modules.register(new FpsCounterModule(hud, themes, adapter));
        modules.register(new PingDisplayModule(hud, themes, adapter));
        modules.register(new ComboCounterModule(hud, themes, adapter));
        modules.register(new ComboTimerModule(hud, themes, adapter));
        modules.register(new StreakCounterModule(hud, themes, adapter));
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
        modules.register(new MaceSmashModule(hud, themes, adapter));
        modules.register(new ShieldStatusModule(hud, themes, adapter));
        modules.register(new ShieldDurabilityModule(hud, themes, adapter));
        modules.register(new ShieldBreakAlertModule(adapter, client.notifications()));
        modules.register(new PearlCooldownModule(hud, themes, adapter));
        modules.register(new PearlLandingMarkerModule(hud, themes, adapter));
        modules.register(new GappleCooldownModule(hud, themes, adapter));
        modules.register(new WindChargeCooldownModule(hud, themes, adapter));
        modules.register(new ChorusCooldownModule(hud, themes, adapter));
        modules.register(new TotemCounterModule(hud, themes, adapter));
        modules.register(new ReachHudModule(hud, themes, adapter));
        modules.register(new CritIndicatorModule(hud, themes, adapter));
        modules.register(new KnockbackIndicatorModule(hud, themes, adapter));
        modules.register(new DuelTimerModule(hud, themes, adapter));
        modules.register(new OffhandHudModule(hud, themes, adapter));
        modules.register(new FoodLevelModule(hud, themes, adapter));
        modules.register(new ObsidianSupplyModule(hud, themes, adapter));
        modules.register(new CrystalSupplyModule(hud, themes, adapter));
        modules.register(new CpvpSupplyModule(hud, themes, adapter));
        modules.register(new ElytraStatusModule(hud, themes, adapter));
        modules.register(new HealthAlertModule(adapter, client.notifications()));
        modules.register(new TotemAlertModule(adapter, client.notifications()));

        // Survival (30)
        modules.register(new DayTimeModule(hud, themes, adapter));
        modules.register(new WeatherHudModule(hud, themes, adapter));
        modules.register(new LightLevelModule(hud, themes, adapter));
        modules.register(new MobSpawnSafeModule(hud, themes, adapter));
        modules.register(new CropGrowthHudModule(hud, themes, adapter));
        modules.register(new RaidAlertModule(adapter, client.notifications()));
        modules.register(new ElytraFlightHudModule(hud, themes, adapter));
        modules.register(new StructureLogModule(hud, themes, adapter));
        modules.register(new VillagerTradeLogModule(hud, themes));
        modules.register(new TeamTagHudModule(hud, themes, adapter));
        modules.register(new BaseRadiusModule(adapter, client.notifications()));
        modules.register(new FriendDeathPingModule(client.notifications()));
        modules.register(new DepthHudModule(hud, themes, adapter));
        modules.register(new ToolDurabilityModule(hud, themes, adapter));
        modules.register(new SaturationHudModule(hud, themes, adapter));
        modules.register(new SpawnDistanceModule(hud, themes, adapter));
        modules.register(new DeathCounterModule(hud, themes));
        modules.register(new BedReminderModule(adapter, client.notifications()));
        modules.register(new WaypointsModule(hud, themes, adapter));
        modules.register(new DeathWaypointModule(hud, themes, adapter));
        modules.register(new ShopWaypointModule(hud, themes, adapter));
        modules.register(new DeathCostModule(hud, themes, adapter));
        modules.register(new BiomeCoordsModule(hud, themes, adapter));
        modules.register(new SpawnCompassModule(hud, themes, adapter));
        modules.register(new ChunkCoordsModule(hud, themes, adapter));
        modules.register(new NetherLinkModule(hud, themes, adapter));
        modules.register(new TravelEtaModule(hud, themes, adapter));
        modules.register(new ServerSessionModule(hud, themes, adapter));
        modules.register(new AfkAlertModule(adapter, client.notifications()));
        modules.register(new RepairAlertModule(adapter, client.notifications()));

        // Performance (12)
        modules.register(new FpsBoosterModule(adapter));
        modules.register(new EntityCullingModule(adapter));
        modules.register(new ParticleOptimizerModule(adapter));
        modules.register(new MemoryMonitorModule(hud, themes, adapter));
        modules.register(new MemorySpikeAlertModule(adapter, client.notifications()));
        modules.register(new AdaptiveFpsModule(adapter));
        modules.register(new RamCleanerModule(adapter));
        modules.register(new DynamicFpsModule(adapter));
        modules.register(new ChunkOptimizerModule(adapter));
        modules.register(new AnimationOptimizerModule(adapter));
        modules.register(new FastLoadingModule(adapter));
        modules.register(new PerformanceProfilesModule(adapter));

        // QoL (17)
        modules.register(new ZoomModule(adapter));
        modules.register(new FullbrightModule(adapter));
        modules.register(new ToggleSprintModule(adapter));
        modules.register(new ToggleSneakModule(adapter));
        modules.register(new AutoRespawnModule(adapter));
        modules.register(new AutoGgModule(adapter));
        modules.register(new SessionRecapModule(adapter, client.notifications()));
        modules.register(new MentionHighlightModule(adapter));
        modules.register(new DeathReplayModule(adapter, client.notifications()));
        modules.register(new ChatTimestampModule());
        modules.register(new BetterChatModule());
        modules.register(new ChatFilterModule());
        modules.register(new ItemCounterModule(hud, themes, adapter));
        modules.register(new ShulkerPreviewModule(hud, themes, adapter));
        modules.register(new BetterTooltipsModule(hud, themes, adapter));
        modules.register(new InventorySearchModule(hud, themes, adapter));
        modules.register(new ServerSwitcherModule(hud, themes, adapter));

        // Creator (9)
        modules.register(new CinematicCameraModule(hud, themes, adapter));
        modules.register(new CinematicGridModule(hud));
        modules.register(new ScreenshotModeModule(adapter));
        modules.register(new StreamerModeModule(hud, adapter));
        modules.register(new StreamSafeHudModule(hud));
        modules.register(new CameraZoomModule(adapter));
        modules.register(new ReplayToolsModule(hud, themes, adapter, client.replaySession(), client.replayStorage()));
        modules.register(new ClipToolsModule(hud, themes, adapter, client.clipRecorder(), client.keybinds()));
        modules.register(new ClipBookmarkModule(client.clipRecorder(), client.replaySession(),
                client.keybinds(), client.notifications()));

        // Prime (10)
        modules.register(new PrimeProfilesModule(client.profiles()));
        modules.register(new ModuleBundlesModule(modules, client.notifications()));
        modules.register(new SmartProfileModule(adapter, client.profiles(), client.notifications()));
        modules.register(new GameplayDnaModule(modules, client.notifications()));
        modules.register(new ServerNotesModule(adapter, client.notifications()));
        modules.register(new PrimeConfigCloudModule(client.cloudSync(), client.profiles()));
        modules.register(new PrimeCosmeticsModule(client.cosmetics()));
        modules.register(new PrimeAccountModule(hud, themes, adapter, client.account()));
        modules.register(new DiscordRichPresenceModule(client.discordRpc(), adapter, modules, client.account()));
        modules.register(new VoiceChatModule(client.voiceChat(), hud, themes, adapter,
                client.notifications(), client.keybinds()));
        modules.register(new PrimeSettingsManagerModule(modules, adapter));
    }
}
