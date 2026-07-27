package dev.primeclient.core.modules.prime;

import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.skin.CustomSkinService;
import dev.primeclient.core.state.CustomSkinState;

/** Custom body skin visible locally and to other Prime Client users. */
public final class CustomSkinModule extends Module {

    private final CustomSkinService skins;
    private int pollTicks;

    public CustomSkinModule(CustomSkinService skins) {
        super("custom-skin", "Custom Skin",
                "Set a custom skin visible to Prime peers (works offline/cracked)",
                ModuleCategory.PRIME);
        this.skins = skins;
        listen(ClientTickEvent.class, event -> {
            if (!isEnabled()) {
                return;
            }
            pollTicks++;
            if (pollTicks >= 40) {
                pollTicks = 0;
                skins.pollBridgeFile();
            }
        });
    }

    @Override
    protected void onEnable() {
        skins.setEnabled(true);
        skins.loadFromDisk();
        CustomSkinState.markAnnounceDirty();
    }

    @Override
    protected void onDisable() {
        skins.setEnabled(false);
    }
}
