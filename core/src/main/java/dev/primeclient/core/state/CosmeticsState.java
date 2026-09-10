package dev.primeclient.core.state;

import dev.primeclient.core.cosmetics.CosmeticLoadout;
import dev.primeclient.core.cosmetics.CosmeticTextures;
import dev.primeclient.core.cosmetics.CosmeticsSettings;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Client-side cosmetic overrides, read by version-layer render hooks. */
public final class CosmeticsState {

    /** Prefer cape IDs via {@link #localLoadout()}. Kept for older callers. */
    @Deprecated
    public enum CapeStyle {
        NONE,
        PRIME,
        STAR
    }

    private static CosmeticLoadout localLoadout = CosmeticLoadout.EMPTY;
    private static final Map<UUID, CosmeticLoadout> peers = new ConcurrentHashMap<>();
    private static final AtomicBoolean announceDirty = new AtomicBoolean(false);
    private static final AtomicReference<CosmeticsSettings> settingsRef =
            new AtomicReference<>(new CosmeticsSettings());

    private CosmeticsState() {
    }

    public static void bindSettings(CosmeticsSettings settings) {
        if (settings != null) {
            settingsRef.set(settings);
        }
    }

    public static CosmeticsSettings settings() {
        return settingsRef.get();
    }

    public static CosmeticLoadout localLoadout() {
        return localLoadout;
    }

    public static String localCapeId() {
        return localLoadout.capeId();
    }

    public static String localWingsId() {
        return localLoadout.wingsId();
    }

    public static String localAuraId() {
        return localLoadout.auraId();
    }

    public static String localTrailId() {
        return localLoadout.trailId();
    }

    public static String localHatId() {
        return localLoadout.hatId();
    }

    public static String localBadgeId() {
        return localLoadout.badgeId();
    }

    public static void setLocalLoadout(String capeId, String wingsId) {
        setLocalLoadout(new CosmeticLoadout(
                capeId, wingsId,
                localLoadout.auraId(), localLoadout.trailId(),
                localLoadout.hatId(), localLoadout.badgeId()));
    }

    public static void setLocalLoadout(CosmeticLoadout loadout) {
        CosmeticLoadout next = sanitize(loadout != null ? loadout : CosmeticLoadout.EMPTY);
        if (!next.equals(localLoadout)) {
            localLoadout = next;
            announceDirty.set(true);
        }
    }

    /** Loadout for rendering: local player always uses local equipment. */
    public static CosmeticLoadout loadoutFor(UUID uuid, boolean localPlayer) {
        if (localPlayer) {
            return localLoadout;
        }
        if (uuid == null) {
            return CosmeticLoadout.EMPTY;
        }
        return peers.getOrDefault(uuid, CosmeticLoadout.EMPTY);
    }

    public static void setPeerLoadout(UUID uuid, String capeId, String wingsId) {
        setPeerLoadout(uuid, new CosmeticLoadout(capeId, wingsId));
    }

    public static void setPeerLoadout(UUID uuid, CosmeticLoadout loadout) {
        if (uuid == null) {
            return;
        }
        peers.put(uuid, sanitize(loadout != null ? loadout : CosmeticLoadout.EMPTY));
    }

    public static void clearPeers() {
        peers.clear();
    }

    public static boolean consumeAnnounceDirty() {
        return announceDirty.getAndSet(false);
    }

    public static void markAnnounceDirty() {
        announceDirty.set(true);
    }

    public static CapeStyle capeStyle() {
        String id = localLoadout.capeId();
        if (id.isBlank()) {
            return CapeStyle.NONE;
        }
        if ("cape-star".equals(id)) {
            return CapeStyle.STAR;
        }
        return CapeStyle.PRIME;
    }

    public static int accentTint() {
        return 0;
    }

    public static void setCapeStyle(CapeStyle style) {
        if (style == null || style == CapeStyle.NONE) {
            setLocalLoadout("", localLoadout.wingsId());
        } else if (style == CapeStyle.STAR) {
            setLocalLoadout("cape-star", localLoadout.wingsId());
        } else {
            setLocalLoadout("cape-prime-classic", localLoadout.wingsId());
        }
    }

    public static void setAccentTint(int tintArgb) {
        // Tint comes from catalog items; API kept for compatibility.
    }

    public static void reset() {
        localLoadout = CosmeticLoadout.EMPTY;
        peers.clear();
        announceDirty.set(false);
        CapePhysicsState.reset();
        EmoteState.reset();
    }

    private static CosmeticLoadout sanitize(CosmeticLoadout in) {
        return new CosmeticLoadout(
                CosmeticTextures.isKnownCape(in.capeId()) ? in.capeId() : "",
                CosmeticTextures.isKnownWings(in.wingsId()) ? in.wingsId() : "",
                CosmeticTextures.isKnownAura(in.auraId()) ? in.auraId() : "",
                CosmeticTextures.isKnownTrail(in.trailId()) ? in.trailId() : "",
                CosmeticTextures.isKnownHat(in.hatId()) ? in.hatId() : "",
                CosmeticTextures.isKnownBadge(in.badgeId()) ? in.badgeId() : "");
    }
}
