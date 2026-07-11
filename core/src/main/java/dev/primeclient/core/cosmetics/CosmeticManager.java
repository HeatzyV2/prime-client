package dev.primeclient.core.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.primeclient.core.config.ConfigBinding;
import dev.primeclient.core.state.CosmeticsState;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Equipment manager for client-side cosmetics. */
public final class CosmeticManager implements ConfigBinding {

    private final Map<String, CosmeticItem> catalog = new LinkedHashMap<>();
    private final EnumMap<CosmeticType, String> equipped = new EnumMap<>(CosmeticType.class);

    public CosmeticManager() {
        register(new CosmeticItem("cape-prime", "Prime Cape", CosmeticType.CAPE, CosmeticItem.Rarity.EPIC, 0xFF3B82F6));
        register(new CosmeticItem("cape-star", "Star Cape", CosmeticType.CAPE, CosmeticItem.Rarity.RARE, 0xFFFFD700));
        register(new CosmeticItem("wings-light", "Light Wings", CosmeticType.WINGS, CosmeticItem.Rarity.LEGENDARY, 0xFFAAEEFF));
        register(new CosmeticItem("hat-crown", "Prime Crown", CosmeticType.HAT, CosmeticItem.Rarity.EPIC, 0xFFFFAA00));
        register(new CosmeticItem("badge-founder", "Founder Badge", CosmeticType.BADGE, CosmeticItem.Rarity.LEGENDARY, 0xFF22C55E));
        equip(CosmeticType.CAPE, "cape-prime");
    }

    public void register(CosmeticItem item) {
        catalog.put(item.id(), item);
    }

    public Map<String, CosmeticItem> catalog() {
        return catalog;
    }

    public void equip(CosmeticType type, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            equipped.remove(type);
        } else if (catalog.containsKey(itemId)) {
            equipped.put(type, itemId);
        }
        syncState();
    }

    public CosmeticItem equipped(CosmeticType type) {
        String id = equipped.get(type);
        return id == null ? null : catalog.get(id);
    }

    private void syncState() {
        CosmeticItem cape = equipped(CosmeticType.CAPE);
        if (cape == null) {
            CosmeticsState.setCapeStyle(CosmeticsState.CapeStyle.NONE);
        } else if ("cape-star".equals(cape.id())) {
            CosmeticsState.setCapeStyle(CosmeticsState.CapeStyle.STAR);
        } else {
            CosmeticsState.setCapeStyle(CosmeticsState.CapeStyle.PRIME);
        }
        CosmeticsState.setAccentTint(cape != null ? cape.tintArgb() : 0);
    }

    @Override
    public String configKey() {
        return "cosmetics";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (Map.Entry<CosmeticType, String> entry : equipped.entrySet()) {
            json.addProperty(entry.getKey().name(), entry.getValue());
        }
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        for (CosmeticType type : CosmeticType.values()) {
            if (json.has(type.name())) {
                equip(type, json.get(type.name()).getAsString());
            }
        }
    }
}
