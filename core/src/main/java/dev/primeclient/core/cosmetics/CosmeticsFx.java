package dev.primeclient.core.cosmetics;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.state.CosmeticsState;
import dev.primeclient.core.state.EmoteState;

import java.util.Locale;
import java.util.UUID;

/** Spawns aura / trail particles with distance cull + quality budget. */
public final class CosmeticsFx {

    private static double lastX = Double.NaN;
    private static double lastZ = Double.NaN;

    private CosmeticsFx() {
    }

    public static void tick(MinecraftAdapter adapter) {
        if (adapter == null || !adapter.hasPlayer() || !adapter.isInGame()) {
            return;
        }
        CosmeticsSettings settings = CosmeticsState.settings();
        if (settings == null || !settings.enabled()) {
            return;
        }
        int budget = settings.particleBudget(adapter.fps());
        if (budget <= 0) {
            return;
        }

        double speed = 0;
        if (!Double.isNaN(lastX)) {
            double dx = adapter.playerX() - lastX;
            double dz = adapter.playerZ() - lastZ;
            speed = Math.sqrt(dx * dx + dz * dz);
        }
        lastX = adapter.playerX();
        lastZ = adapter.playerZ();

        // Emote pose feedback — particle burst while playing
        EmoteState.ActiveEmote localEmote = EmoteState.local();
        if (localEmote != null && settings.showOwn()) {
            adapter.spawnHitParticles(
                    adapter.playerX(), adapter.playerY() + 1.2, adapter.playerZ(),
                    0xFFFBBF24, 0.9f, Math.min(4, Math.max(1, budget / 4)));
        }

        if (settings.showOwn()) {
            spawnFor(adapter, CosmeticsState.localLoadout(),
                    adapter.playerX(), adapter.playerY(), adapter.playerZ(),
                    speed, budget / 2);
        }

        if (!settings.showOthers()) {
            return;
        }
        int count = adapter.onlinePlayerCount();
        if (count <= 0) {
            return;
        }
        int perPeer = Math.max(1, budget / Math.max(1, count));
        for (int i = 0; i < count; i++) {
            String raw = adapter.onlinePlayerUuid(i);
            if (raw == null || raw.isBlank() || raw.equals(adapter.playerUuid())) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(raw);
                if (!PrimeClient.get().presence().isPrime(uuid)) {
                    continue;
                }
                double dist = adapter.distanceToPlayer(raw);
                if (dist > settings.maxDistance()) {
                    continue;
                }
                double x = adapter.playerXForUuid(raw);
                double y = adapter.playerYForUuid(raw);
                double z = adapter.playerZForUuid(raw);
                if (Double.isNaN(x)) {
                    continue;
                }
                spawnFor(adapter, CosmeticsState.loadoutFor(uuid, false), x, y, z, 0.12, perPeer);
            } catch (Exception ignored) {
            }
        }
    }

    private static void spawnFor(
            MinecraftAdapter adapter,
            CosmeticLoadout loadout,
            double x, double y, double z,
            double speed,
            int budget) {
        if (loadout == null || budget <= 0) {
            return;
        }

        int auraBudget = loadout.hasAura() ? Math.max(1, budget / 2) : 0;
        int trailBudget = loadout.hasTrail() ? Math.max(1, budget - auraBudget) : 0;

        if (auraBudget > 0) {
            int color = tintFor(loadout.auraId(), 0xFF3B82F6);
            float size = loadout.auraId().contains("void") ? 1.2f : 0.85f;
            adapter.spawnHitParticles(x, y + 1.0, z, color, size, Math.min(8, auraBudget));
        }
        if (trailBudget > 0 && speed >= 0.02) {
            int color = tintFor(loadout.trailId(), 0xFFFFD700);
            adapter.spawnHitParticles(x, y + 0.15, z, color, 0.65f, Math.min(6, trailBudget));
        }
    }

    private static int tintFor(String id, int fallback) {
        if (id == null) {
            return fallback;
        }
        String key = id.toLowerCase(Locale.ROOT);
        if (key.contains("fire") || key.contains("flame") || key.contains("inferno")) {
            return 0xFFFF4500;
        }
        if (key.contains("void") || key.contains("shadow")) {
            return 0xFF312E81;
        }
        if (key.contains("lightning")) {
            return 0xFFFBBF24;
        }
        if (key.contains("royal") || key.contains("star")) {
            return 0xFFF59E0B;
        }
        if (key.contains("rainbow")) {
            return 0xFFEC4899;
        }
        if (key.contains("neon") || key.contains("aurora")) {
            return 0xFF22D3EE;
        }
        return fallback;
    }
}
