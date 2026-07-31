package dev.primeclient.core.state;

import dev.primeclient.core.cosmetics.EmoteCatalog;
import dev.primeclient.core.cosmetics.EmoteDefinition;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Active emote playback for local player and Prime peers. */
public final class EmoteState {

    public record ActiveEmote(String emoteId, long startedAtMs, int durationTicks) {
        public float progress(long nowMs) {
            float durationMs = Math.max(1, durationTicks) * 50f;
            return Math.max(0f, Math.min(1f, (nowMs - startedAtMs) / durationMs));
        }

        public boolean finished(long nowMs) {
            return progress(nowMs) >= 1f;
        }

        public EmoteDefinition.Pose pose(long nowMs) {
            EmoteDefinition def = EmoteCatalog.get(emoteId);
            if (def == null) {
                return EmoteDefinition.Pose.NEUTRAL;
            }
            return def.sample(progress(nowMs));
        }
    }

    private static final AtomicReference<ActiveEmote> local = new AtomicReference<>();
    private static final Map<UUID, ActiveEmote> peers = new ConcurrentHashMap<>();
    private static Consumer<String> networkAnnouncer = id -> {};
    private static boolean wheelOpen;

    private EmoteState() {
    }

    public static void setNetworkAnnouncer(Consumer<String> announcer) {
        networkAnnouncer = announcer != null ? announcer : id -> {};
    }

    public static void playLocal(String emoteId) {
        if (!EmoteCatalog.isKnown(emoteId)) {
            return;
        }
        EmoteDefinition def = EmoteCatalog.get(emoteId);
        ActiveEmote active = new ActiveEmote(emoteId, System.currentTimeMillis(), def.durationTicks());
        local.set(active);
        networkAnnouncer.accept(emoteId);
    }

    public static void playPeer(UUID uuid, String emoteId, long startedAtMs) {
        if (uuid == null || !EmoteCatalog.isKnown(emoteId)) {
            return;
        }
        EmoteDefinition def = EmoteCatalog.get(emoteId);
        long start = startedAtMs > 0 ? startedAtMs : System.currentTimeMillis();
        peers.put(uuid, new ActiveEmote(emoteId, start, def.durationTicks()));
    }

    public static ActiveEmote local() {
        return prune(local.get(), null);
    }

    public static ActiveEmote forPlayer(UUID uuid, boolean localPlayer) {
        if (localPlayer) {
            return local();
        }
        if (uuid == null) {
            return null;
        }
        return prune(peers.get(uuid), uuid);
    }

    public static void tick() {
        prune(local.get(), null);
        long now = System.currentTimeMillis();
        peers.entrySet().removeIf(e -> e.getValue().finished(now));
    }

    public static boolean wheelOpen() {
        return wheelOpen;
    }

    public static void setWheelOpen(boolean open) {
        wheelOpen = open;
    }

    public static void reset() {
        local.set(null);
        peers.clear();
        wheelOpen = false;
    }

    private static ActiveEmote prune(ActiveEmote active, UUID peer) {
        if (active == null) {
            return null;
        }
        if (active.finished(System.currentTimeMillis())) {
            if (peer == null) {
                local.compareAndSet(active, null);
            } else {
                peers.remove(peer, active);
            }
            return null;
        }
        return active;
    }
}
