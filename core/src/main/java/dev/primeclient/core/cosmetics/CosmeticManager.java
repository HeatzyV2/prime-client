package dev.primeclient.core.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.primeclient.core.config.ConfigBinding;
import dev.primeclient.core.state.CosmeticsState;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Equipment manager for client-side cosmetics (all slots unlocked for Prime). */
public final class CosmeticManager implements ConfigBinding {

    private static final EnumSet<CosmeticType> EQUIP_SLOTS = EnumSet.of(
            CosmeticType.CAPE, CosmeticType.WINGS, CosmeticType.AURA,
            CosmeticType.TRAIL, CosmeticType.HAT, CosmeticType.BADGE);

    private final Map<String, CosmeticItem> catalog = new LinkedHashMap<>();
    private final EnumMap<CosmeticType, String> equipped = new EnumMap<>(CosmeticType.class);
    private final Set<String> favorites = new LinkedHashSet<>();
    private final Set<String> owned = new LinkedHashSet<>();
    private final CosmeticsSettings settings = new CosmeticsSettings();

    public CosmeticManager() {
        seedCatalog();
        // Prime users own everything by default (no paywall).
        owned.addAll(catalog.keySet());
        equip(CosmeticType.CAPE, "cape-prime-classic");
        equip(CosmeticType.WINGS, "wings-prime");
    }

    private void seedCatalog() {
        // Capes — Prime 4 + legacy
        register(item("cape-prime-classic", "Prime Classic", "Signature blue Prime cape",
                CosmeticType.CAPE, CosmeticItem.Rarity.LEGENDARY, 0xFF3B82F6));
        register(item("cape-prime-founder", "Prime Founder", "Founder exclusive cape",
                CosmeticType.CAPE, CosmeticItem.Rarity.PRIME_EXCLUSIVE, 0xFFF59E0B));
        register(item("cape-prime-neon", "Prime Neon", "Electric neon cape",
                CosmeticType.CAPE, CosmeticItem.Rarity.MYTHIC, 0xFF22D3EE));
        register(item("cape-prime-shadow", "Prime Shadow", "Void shadow cape",
                CosmeticType.CAPE, CosmeticItem.Rarity.EPIC, 0xFF64748B));
        register(item("cape-prime", "Prime Cape", "Legacy alias of Classic",
                CosmeticType.CAPE, CosmeticItem.Rarity.LEGENDARY, 0xFF3B82F6));
        register(item("cape-star", "Star Cape", "Gold star cape",
                CosmeticType.CAPE, CosmeticItem.Rarity.EPIC, 0xFFFFD700));
        register(item("cape-crimson", "Crimson Cape", "Crimson signature cape",
                CosmeticType.CAPE, CosmeticItem.Rarity.EPIC, 0xFFE11D48));
        register(item("cape-midnight", "Midnight Cape", "Indigo midnight cape",
                CosmeticType.CAPE, CosmeticItem.Rarity.RARE, 0xFF6366F1));

        // Wings
        register(item("wings-inferno", "Inferno Wings", "Blazing inferno wings",
                CosmeticType.WINGS, CosmeticItem.Rarity.MYTHIC, 0xFFFF4500));
        register(item("wings-shadow", "Shadow Wings", "Dark ethereal wings",
                CosmeticType.WINGS, CosmeticItem.Rarity.EPIC, 0xFF4B5563));
        register(item("wings-galaxy", "Galaxy Wings", "Cosmic galaxy wings",
                CosmeticType.WINGS, CosmeticItem.Rarity.LEGENDARY, 0xFF8B5CF6));
        register(item("wings-prime", "Prime Wings", "Official Prime wings",
                CosmeticType.WINGS, CosmeticItem.Rarity.LEGENDARY, 0xFF3B82F6));
        register(item("wings-ember", "Ember Wings", "Fiery ember wings",
                CosmeticType.WINGS, CosmeticItem.Rarity.LEGENDARY, 0xFFFF6B35));
        register(item("wings-aurora", "Aurora Wings", "Aurora light wings",
                CosmeticType.WINGS, CosmeticItem.Rarity.EPIC, 0xFF22D3EE));

        // Auras
        register(item("aura-prime-energy", "Prime Energy", "Blue energy aura",
                CosmeticType.AURA, CosmeticItem.Rarity.LEGENDARY, 0xFF3B82F6));
        register(item("aura-fire", "Fire Aura", "Flame particles",
                CosmeticType.AURA, CosmeticItem.Rarity.EPIC, 0xFFFF6B35));
        register(item("aura-void", "Void Aura", "Dark void swirl",
                CosmeticType.AURA, CosmeticItem.Rarity.MYTHIC, 0xFF1E1B4B));
        register(item("aura-lightning", "Lightning Aura", "Electric sparks",
                CosmeticType.AURA, CosmeticItem.Rarity.EPIC, 0xFFFBBF24));
        register(item("aura-royal", "Royal Aura", "Golden royal glow",
                CosmeticType.AURA, CosmeticItem.Rarity.PRIME_EXCLUSIVE, 0xFFF59E0B));

        // Trails
        register(item("trail-flame", "Flame Trail", "Fire trail while moving",
                CosmeticType.TRAIL, CosmeticItem.Rarity.EPIC, 0xFFFF4500));
        register(item("trail-star", "Star Trail", "Sparkling star trail",
                CosmeticType.TRAIL, CosmeticItem.Rarity.RARE, 0xFFFFD700));
        register(item("trail-rainbow", "Rainbow Trail", "Rainbow particle trail",
                CosmeticType.TRAIL, CosmeticItem.Rarity.LEGENDARY, 0xFFEC4899));
        register(item("trail-shadow", "Shadow Trail", "Dark smoke trail",
                CosmeticType.TRAIL, CosmeticItem.Rarity.EPIC, 0xFF475569));
        register(item("trail-prime", "Prime Trail", "Prime blue trail",
                CosmeticType.TRAIL, CosmeticItem.Rarity.LEGENDARY, 0xFF3B82F6));

        // Hats
        register(item("hat-crown", "Prime Crown", "Royal crown hat",
                CosmeticType.HAT, CosmeticItem.Rarity.LEGENDARY, 0xFFFBBF24));
        register(item("hat-horns", "Dragon Horns", "Dragon horn headpiece",
                CosmeticType.HAT, CosmeticItem.Rarity.EPIC, 0xFFDC2626));
        register(item("hat-wizard", "Wizard Hat", "Classic wizard hat",
                CosmeticType.HAT, CosmeticItem.Rarity.RARE, 0xFF7C3AED));
        register(item("hat-santa", "Santa Hat", "Festive santa hat",
                CosmeticType.HAT, CosmeticItem.Rarity.COMMON, 0xFFEF4444));
        register(item("hat-dev", "Developer Cap", "Dev-only style cap",
                CosmeticType.HAT, CosmeticItem.Rarity.PRIME_EXCLUSIVE, 0xFF22C55E));

        // Emotes
        register(item("emote-wave", "Wave", "Friendly wave",
                CosmeticType.EMOTE, CosmeticItem.Rarity.COMMON, 0xFF60A5FA));
        register(item("emote-dance", "Dance", "Dance loop",
                CosmeticType.EMOTE, CosmeticItem.Rarity.RARE, 0xFFEC4899));
        register(item("emote-sit", "Sit", "Sit pose",
                CosmeticType.EMOTE, CosmeticItem.Rarity.COMMON, 0xFF94A3B8));
        register(item("emote-laugh", "Laugh", "Laugh animation",
                CosmeticType.EMOTE, CosmeticItem.Rarity.RARE, 0xFFFBBF24));
        register(item("emote-cry", "Cry", "Sad cry",
                CosmeticType.EMOTE, CosmeticItem.Rarity.COMMON, 0xFF38BDF8));
        register(item("emote-flex", "Flex", "Flex muscles",
                CosmeticType.EMOTE, CosmeticItem.Rarity.EPIC, 0xFFEF4444));
        register(item("emote-clap", "Clap", "Applause",
                CosmeticType.EMOTE, CosmeticItem.Rarity.COMMON, 0xFFA3E635));
        register(item("emote-sleep", "Sleep", "Sleep pose",
                CosmeticType.EMOTE, CosmeticItem.Rarity.RARE, 0xFF818CF8));
        register(item("emote-victory", "Victory", "Victory celebration",
                CosmeticType.EMOTE, CosmeticItem.Rarity.LEGENDARY, 0xFFF59E0B));

        // Badges
        register(item("badge-founder", "Founder", "Early adopter badge",
                CosmeticType.BADGE, CosmeticItem.Rarity.PRIME_EXCLUSIVE, 0xFFF59E0B));
        register(item("badge-creator", "Creator", "Content creator badge",
                CosmeticType.BADGE, CosmeticItem.Rarity.LEGENDARY, 0xFFEC4899));
        register(item("badge-partner", "Partner", "Partner server badge",
                CosmeticType.BADGE, CosmeticItem.Rarity.EPIC, 0xFF3B82F6));
        register(item("badge-supporter", "Supporter", "Community supporter",
                CosmeticType.BADGE, CosmeticItem.Rarity.RARE, 0xFF22C55E));
    }

    private static CosmeticItem item(String id, String name, String desc,
                                    CosmeticType type, CosmeticItem.Rarity rarity, int tint) {
        return new CosmeticItem(id, name, desc, type, rarity, tint);
    }

    public void register(CosmeticItem item) {
        catalog.put(item.id(), item);
    }

    public Map<String, CosmeticItem> catalog() {
        return catalog;
    }

    public CosmeticsSettings settings() {
        return settings;
    }

    public Set<String> favorites() {
        return Collections.unmodifiableSet(favorites);
    }

    public boolean isFavorite(String id) {
        return id != null && favorites.contains(id);
    }

    public void toggleFavorite(String id) {
        if (id == null || !catalog.containsKey(id)) {
            return;
        }
        if (!favorites.add(id)) {
            favorites.remove(id);
        }
    }

    public boolean isOwned(String id) {
        return id != null && owned.contains(id);
    }

    public void equip(CosmeticType type, String itemId) {
        if (!EQUIP_SLOTS.contains(type)) {
            return;
        }
        if (itemId == null || itemId.isBlank()) {
            equipped.remove(type);
            syncState();
            return;
        }
        String resolved = CosmeticTextures.canonicalize(itemId);
        if ("wings-light".equals(itemId)) {
            resolved = "wings-aurora";
        }
        if (!catalog.containsKey(resolved) && catalog.containsKey(itemId)) {
            resolved = itemId;
        }
        if (catalog.containsKey(resolved)) {
            equipped.put(type, resolved);
        }
        syncState();
    }

    public CosmeticItem equipped(CosmeticType type) {
        String id = equipped.get(type);
        return id == null ? null : catalog.get(id);
    }

    public void unequip(CosmeticType type) {
        if (!EQUIP_SLOTS.contains(type)) {
            return;
        }
        equipped.remove(type);
        syncState();
    }

    public CollectionProgress collectionProgress() {
        EnumMap<CosmeticType, CollectionProgress.TypeStats> byType = new EnumMap<>(CosmeticType.class);
        int ownedTotal = 0;
        int catalogTotal = 0;
        for (CosmeticType type : CosmeticType.values()) {
            int total = 0;
            int own = 0;
            for (CosmeticItem item : catalog.values()) {
                if (item.type() != type) {
                    continue;
                }
                // Skip pure legacy aliases from collection totals when a canonical twin exists
                if ("cape-prime".equals(item.id())) {
                    continue;
                }
                total++;
                if (owned.contains(item.id())) {
                    own++;
                }
            }
            byType.put(type, new CollectionProgress.TypeStats(own, total));
            ownedTotal += own;
            catalogTotal += total;
        }
        return new CollectionProgress(byType, ownedTotal, catalogTotal);
    }

    private void syncState() {
        CosmeticsState.setLocalLoadout(new CosmeticLoadout(
                idOrEmpty(CosmeticType.CAPE),
                idOrEmpty(CosmeticType.WINGS),
                idOrEmpty(CosmeticType.AURA),
                idOrEmpty(CosmeticType.TRAIL),
                idOrEmpty(CosmeticType.HAT),
                idOrEmpty(CosmeticType.BADGE)));
        CosmeticItem cape = equipped(CosmeticType.CAPE);
        CosmeticsState.setAccentTint(cape != null ? cape.tintArgb() : 0);
    }

    private String idOrEmpty(CosmeticType type) {
        String id = equipped.get(type);
        return id == null ? "" : id;
    }

    @Override
    public String configKey() {
        return "cosmetics";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (Map.Entry<CosmeticType, String> entry : equipped.entrySet()) {
            if (EQUIP_SLOTS.contains(entry.getKey())) {
                json.addProperty(entry.getKey().name(), entry.getValue());
            }
        }
        JsonArray fav = new JsonArray();
        for (String id : favorites) {
            fav.add(id);
        }
        json.add("favorites", fav);
        json.add("settings", settings.saveConfig());
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        equipped.clear();
        for (CosmeticType type : EQUIP_SLOTS) {
            if (json.has(type.name())) {
                equip(type, json.get(type.name()).getAsString());
            }
        }
        favorites.clear();
        if (json.has("favorites") && json.get("favorites").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("favorites")) {
                String id = e.getAsString();
                if (catalog.containsKey(id)) {
                    favorites.add(id);
                }
            }
        }
        if (json.has("settings")) {
            settings.loadConfig(json.get("settings"));
        }
        syncState();
    }
}
