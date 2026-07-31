package dev.primeclient.core.cosmetics;

/** One cosmetic item with rarity for inventory / shop / collection. */
public record CosmeticItem(
        String id,
        String name,
        String description,
        CosmeticType type,
        Rarity rarity,
        int tintArgb
) {
    public CosmeticItem(String id, String name, CosmeticType type, Rarity rarity, int tintArgb) {
        this(id, name, "", type, rarity, tintArgb);
    }

    public enum Rarity {
        COMMON,
        RARE,
        EPIC,
        LEGENDARY,
        MYTHIC,
        PRIME_EXCLUSIVE
    }
}
