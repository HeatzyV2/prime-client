package dev.primeclient.core.modules.prime;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.notification.NotificationManager;
import dev.primeclient.core.profile.ProfileManager;

/** Auto-suggest profile from held item context. */
public final class SmartProfileModule extends Module {

    private final BooleanSetting autoApply =
            addSetting(new BooleanSetting("auto-apply", "Auto apply", "Switch profile hints automatically", false));

    private final MinecraftAdapter adapter;
    private final ProfileManager profiles;
    private final NotificationManager notifications;

    private String lastCategory = "";

    public SmartProfileModule(MinecraftAdapter adapter, ProfileManager profiles,
                              NotificationManager notifications) {
        super("smart-profile", "Smart Profile", "Suggest profile from held tool", ModuleCategory.PRIME);
        this.adapter = adapter;
        this.profiles = profiles;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> evaluate());
    }

    private void evaluate() {
        if (!adapter.hasPlayer()) {
            return;
        }
        String category = adapter.heldToolCategory();
        if (category.equals(lastCategory)) {
            return;
        }
        lastCategory = category;
        if (category.isEmpty()) {
            return;
        }
        String preset = switch (category) {
            case "sword", "axe" -> "pvp";
            case "pickaxe", "shovel" -> "survival";
            default -> "";
        };
        if (preset.isEmpty()) {
            return;
        }
        notifications.info("Smart Profile", "Holding " + category + " → try " + preset.toUpperCase() + " profile");
        if (autoApply.get()) {
            profiles.switchTo(preset);
        }
    }
}
