package dev.primeclient.core.modules.prime;

import dev.primeclient.core.cosmetics.CosmeticManager;
import dev.primeclient.core.cosmetics.CosmeticType;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.EnumSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.state.CosmeticsState;

/** Equip Prime cosmetics (capes and wings — world-rendered). */
public final class PrimeCosmeticsModule extends Module {

    private final EnumSetting<CosmeticType> slot =
            addSetting(new EnumSetting<>("slot", "Slot", "Cosmetic slot to edit", CosmeticType.CAPE));

    private final CosmeticManager cosmetics;

    public PrimeCosmeticsModule(CosmeticManager cosmetics) {
        super("prime-cosmetics", "Prime Cosmetics",
                "Capes and wings visible to you and Prime peers", ModuleCategory.PRIME);
        this.cosmetics = cosmetics;
        listen(ClientTickEvent.class, event -> {
            if (isEnabled()) {
                pushState();
            }
        });
    }

    @Override
    protected void onEnable() {
        pushState();
    }

    @Override
    protected void onDisable() {
        CosmeticsState.reset();
    }

    /** Cycles equipped item in the selected slot. */
    public void cycleEquipped() {
        CosmeticType type = slot.get();
        if (type != CosmeticType.CAPE && type != CosmeticType.WINGS) {
            type = CosmeticType.CAPE;
        }
        final CosmeticType cycleType = type;
        var items = cosmetics.catalog().values().stream()
                .filter(i -> i.type() == cycleType).toList();
        if (items.isEmpty()) {
            return;
        }
        var current = cosmetics.equipped(type);
        int idx = 0;
        if (current != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id().equals(current.id())) {
                    idx = (i + 1) % items.size();
                    break;
                }
            }
        }
        cosmetics.equip(type, items.get(idx).id());
    }

    private void pushState() {
        var cape = cosmetics.equipped(CosmeticType.CAPE);
        var wings = cosmetics.equipped(CosmeticType.WINGS);
        CosmeticsState.setLocalLoadout(
                cape != null ? cape.id() : "",
                wings != null ? wings.id() : "");
    }
}
