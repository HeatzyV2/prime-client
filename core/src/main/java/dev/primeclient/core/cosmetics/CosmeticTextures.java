package dev.primeclient.core.cosmetics;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Maps catalog cosmetic IDs to asset paths under {@code assets/primeclient/}. */
public final class CosmeticTextures {

    private static final Map<String, String> CAPE_PATHS = new HashMap<>();
    private static final Map<String, String> WINGS_PATHS = new HashMap<>();
    private static final Map<String, String> HAT_PATHS = new HashMap<>();
    private static final Map<String, String> LEGACY_ALIASES = new HashMap<>();

    static {
        CAPE_PATHS.put("cape-prime-classic", "textures/cosmetics/cape_prime_classic.png");
        CAPE_PATHS.put("cape-prime-founder", "textures/cosmetics/cape_prime_founder.png");
        CAPE_PATHS.put("cape-prime-neon", "textures/cosmetics/cape_prime_neon.png");
        CAPE_PATHS.put("cape-prime-shadow", "textures/cosmetics/cape_prime_shadow.png");
        // Legacy IDs keep working
        CAPE_PATHS.put("cape-prime", "textures/cosmetics/cape_prime_classic.png");
        CAPE_PATHS.put("cape-star", "textures/cosmetics/cape_star.png");
        CAPE_PATHS.put("cape-crimson", "textures/cosmetics/cape_crimson.png");
        CAPE_PATHS.put("cape-midnight", "textures/cosmetics/cape_midnight.png");

        WINGS_PATHS.put("wings-inferno", "textures/cosmetics/wings_inferno.png");
        WINGS_PATHS.put("wings-shadow", "textures/cosmetics/wings_shadow.png");
        WINGS_PATHS.put("wings-galaxy", "textures/cosmetics/wings_galaxy.png");
        WINGS_PATHS.put("wings-prime", "textures/cosmetics/wings_prime.png");
        WINGS_PATHS.put("wings-ember", "textures/cosmetics/wings_ember.png");
        WINGS_PATHS.put("wings-aurora", "textures/cosmetics/wings_aurora.png");
        WINGS_PATHS.put("wings-light", "textures/cosmetics/wings_aurora.png");

        HAT_PATHS.put("hat-crown", "textures/cosmetics/hat_crown.png");
        HAT_PATHS.put("hat-horns", "textures/cosmetics/hat_horns.png");
        HAT_PATHS.put("hat-wizard", "textures/cosmetics/hat_wizard.png");
        HAT_PATHS.put("hat-santa", "textures/cosmetics/hat_santa.png");
        HAT_PATHS.put("hat-dev", "textures/cosmetics/hat_dev.png");

        LEGACY_ALIASES.put("cape-prime", "cape-prime-classic");
        LEGACY_ALIASES.put("wings-light", "wings-aurora");
    }

    private CosmeticTextures() {
    }

    /** Resolves legacy aliases to canonical catalog IDs. */
    public static String canonicalize(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String key = id.toLowerCase(Locale.ROOT);
        return LEGACY_ALIASES.getOrDefault(key, key);
    }

    public static String capePath(String capeId) {
        if (capeId == null || capeId.isBlank()) {
            return null;
        }
        return CAPE_PATHS.get(capeId.toLowerCase(Locale.ROOT));
    }

    public static String wingsPath(String wingsId) {
        if (wingsId == null || wingsId.isBlank()) {
            return null;
        }
        return WINGS_PATHS.get(wingsId.toLowerCase(Locale.ROOT));
    }

    public static String hatPath(String hatId) {
        if (hatId == null || hatId.isBlank()) {
            return null;
        }
        return HAT_PATHS.get(hatId.toLowerCase(Locale.ROOT));
    }

    public static boolean isKnownCape(String capeId) {
        return capePath(capeId) != null;
    }

    public static boolean isKnownWings(String wingsId) {
        return wingsPath(wingsId) != null;
    }

    public static boolean isKnownHat(String hatId) {
        return hatPath(hatId) != null;
    }

    public static boolean isKnownAura(String auraId) {
        return auraId != null && !auraId.isBlank() && auraId.toLowerCase(Locale.ROOT).startsWith("aura-");
    }

    public static boolean isKnownTrail(String trailId) {
        return trailId != null && !trailId.isBlank() && trailId.toLowerCase(Locale.ROOT).startsWith("trail-");
    }

    public static boolean isKnownBadge(String badgeId) {
        return badgeId != null && !badgeId.isBlank() && badgeId.toLowerCase(Locale.ROOT).startsWith("badge-");
    }
}
