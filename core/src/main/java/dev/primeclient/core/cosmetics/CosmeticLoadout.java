package dev.primeclient.core.cosmetics;

/** Equipped cosmetic IDs (empty strings mean none). Emotes are played, not stored here. */
public record CosmeticLoadout(
        String capeId,
        String wingsId,
        String auraId,
        String trailId,
        String hatId,
        String badgeId
) {

    public static final CosmeticLoadout EMPTY = new CosmeticLoadout("", "", "", "", "", "");

    public CosmeticLoadout {
        capeId = nullToEmpty(capeId);
        wingsId = nullToEmpty(wingsId);
        auraId = nullToEmpty(auraId);
        trailId = nullToEmpty(trailId);
        hatId = nullToEmpty(hatId);
        badgeId = nullToEmpty(badgeId);
    }

    public CosmeticLoadout(String capeId, String wingsId) {
        this(capeId, wingsId, "", "", "", "");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public boolean hasCape() {
        return !capeId.isBlank();
    }

    public boolean hasWings() {
        return !wingsId.isBlank();
    }

    public boolean hasAura() {
        return !auraId.isBlank();
    }

    public boolean hasTrail() {
        return !trailId.isBlank();
    }

    public boolean hasHat() {
        return !hatId.isBlank();
    }

    public boolean hasBadge() {
        return !badgeId.isBlank();
    }
}
