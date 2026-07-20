package dev.primeclient.core.presence;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.state.ClientBadgeState;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks Prime Client users discovered via Fabric presence payloads (LAN / integrated server). */
public final class PrimePresenceService {

    private final MinecraftAdapter adapter;
    private final Set<UUID> primeUsers = ConcurrentHashMap.newKeySet();
    private int announceTicks = -1;
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
        announceTicks = 0;
        announceSent = false;
        if (ClientBadgeState.active()) {
            markLocalPlayer();
        }
    }

    public void onWorldLeave() {
        primeUsers.clear();
        announceTicks = -1;
        announceSent = false;
    }

    public void tick() {
        if (!ClientBadgeState.active() || announceSent || announceTicks < 0) {
            return;
        }
        announceTicks++;
        if (announceTicks >= 20) {
            announcePresence();
        }
    }

    public void onModuleEnabled() {
        markLocalPlayer();
        if (adapter.hasPlayer() && !announceSent) {
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
        if (uuid != null) {
            primeUsers.add(uuid);
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
