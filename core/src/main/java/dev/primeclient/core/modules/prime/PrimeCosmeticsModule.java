package dev.primeclient.core.modules.prime;

import dev.primeclient.core.cosmetics.CosmeticManager;
import dev.primeclient.core.cosmetics.CosmeticType;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.EnumSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.state.CosmeticsState;

/** Equip Prime cosmetics (capes, wings, hats, badges). */
public final class PrimeCosmeticsModule extends Module {

    private final EnumSetting<CosmeticType> slot =
            addSetting(new EnumSetting<>("slot", "Slot", "Cosmetic slot to edit", CosmeticType.CAPE));

    private final CosmeticManager cosmetics;
    private CosmeticType lastSlot = CosmeticType.CAPE;

    public PrimeCosmeticsModule(CosmeticManager cosmetics) {
        super("prime-cosmetics", "Prime Cosmetics", "Customize your client appearance", ModuleCategory.PRIME);
        this.cosmetics = cosmetics;
        listen(ClientTickEvent.class, event -> sync());
    }

    @Override
    protected void onEnable() {
        sync();
    }

    @Override
    protected void onDisable() {
        CosmeticsState.reset();
    }

    /** Cycles equipped item in the selected slot. */
    public void cycleEquipped() {
        CosmeticType type = slot.get();
        var items = cosmetics.catalog().values().stream()
                .filter(i -> i.type() == type).toList();
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

    private void sync() {
        if (!isEnabled()) {
            return;
        }
        if (slot.get() != lastSlot) {
            lastSlot = slot.get();
        }
    }
}
