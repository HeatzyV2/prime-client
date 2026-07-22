package dev.primeclient.core.presence;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.state.ClientBadgeState;
import dev.primeclient.core.state.CosmeticsState;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks Prime Client users discovered via Fabric presence payloads (LAN / integrated server). */
public final class PrimePresenceService {

    private static final int INITIAL_ANNOUNCE_TICKS = 20;
    private static final int REANNOUNCE_INTERVAL_TICKS = 100;

    private final MinecraftAdapter adapter;
    private final Set<UUID> primeUsers = ConcurrentHashMap.newKeySet();
    private int announceTicks = -1;
    private int reannounceCooldown;
    private boolean announceSent;
    private Runnable networkAnnouncer = () -> {};

    public PrimePresenceService(MinecraftAdapter adapter) {
        this.adapter = adapter;
    }

    public void setNetworkAnnouncer(Runnable announcer) {
        this.networkAnnouncer = announcer != null ? announcer : () -> {};
    }

    public void onWorldJoin() {
        primeUsers.clear();
        CosmeticsState.clearPeers();
        announceTicks = 0;
        announceSent = false;
        reannounceCooldown = 0;
        if (ClientBadgeState.active()) {
            markLocalPlayer();
        }
    }

    public void onWorldLeave() {
        primeUsers.clear();
        CosmeticsState.clearPeers();
        announceTicks = -1;
        announceSent = false;
        reannounceCooldown = 0;
    }

    public void tick() {
        if (!ClientBadgeState.active() || announceTicks < 0) {
            CosmeticsState.consumeAnnounceDirty();
            return;
        }
        if (!announceSent) {
            announceTicks++;
            if (announceTicks >= INITIAL_ANNOUNCE_TICKS) {
                announcePresence();
            }
            return;
        }
        if (CosmeticsState.consumeAnnounceDirty()) {
            announcePresence();
            reannounceCooldown = REANNOUNCE_INTERVAL_TICKS;
            return;
        }
        if (reannounceCooldown > 0) {
            reannounceCooldown--;
            return;
        }
        announcePresence();
        reannounceCooldown = REANNOUNCE_INTERVAL_TICKS;
    }

    public void onModuleEnabled() {
        markLocalPlayer();
        announceSent = false;
        announceTicks = 0;
        if (adapter.hasPlayer()) {
            announcePresence();
        }
    }

    public void onModuleDisabled() {
        UUID local = localUuid();
        if (local != null) {
            primeUsers.remove(local);
        }
    }

    public boolean isPrime(UUID uuid) {
        return uuid != null && primeUsers.contains(uuid);
    }

    public void markPrime(UUID uuid) {
        markPrime(uuid, "", "");
    }

    public void markPrime(UUID uuid, String capeId, String wingsId) {
        if (uuid == null) {
            return;
        }
        primeUsers.add(uuid);
        CosmeticsState.setPeerLoadout(uuid, capeId, wingsId);
    }

    /** Force a presence broadcast (e.g. after equipping cosmetics). */
    public void requestAnnounce() {
        CosmeticsState.markAnnounceDirty();
        if (ClientBadgeState.active() && adapter.hasPlayer()) {
            announcePresence();
            reannounceCooldown = REANNOUNCE_INTERVAL_TICKS;
        }
    }

    private void announcePresence() {
        if (localUuid() == null) {
            return;
        }
        networkAnnouncer.run();
        announceSent = true;
    }

    private void markLocalPlayer() {
        UUID local = localUuid();
        if (local != null) {
            primeUsers.add(local);
            CosmeticsState.setPeerLoadout(
                    local, CosmeticsState.localCapeId(), CosmeticsState.localWingsId());
        }
    }

    private UUID localUuid() {
        String raw = adapter.playerUuid();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
