package dev.primeclient.core.cosmetics;

import java.util.EnumMap;
import java.util.Map;

/** Owned / total counts per cosmetic type for the Collection panel. */
public final class CollectionProgress {

    public record TypeStats(int owned, int total) {
        public float percent() {
            return total <= 0 ? 0f : (owned * 100f) / total;
        }
    }

    private final EnumMap<CosmeticType, TypeStats> byType = new EnumMap<>(CosmeticType.class);
    private final int ownedTotal;
    private final int catalogTotal;

    public CollectionProgress(Map<CosmeticType, TypeStats> byType, int ownedTotal, int catalogTotal) {
        this.byType.putAll(byType);
        this.ownedTotal = ownedTotal;
        this.catalogTotal = catalogTotal;
    }

    public TypeStats forType(CosmeticType type) {
        return byType.getOrDefault(type, new TypeStats(0, 0));
    }

    public Map<CosmeticType, TypeStats> byType() {
        return byType;
    }

    public int ownedTotal() {
        return ownedTotal;
    }

    public int catalogTotal() {
        return catalogTotal;
    }

    public float overallPercent() {
        return catalogTotal <= 0 ? 0f : (ownedTotal * 100f) / catalogTotal;
    }
}
