package dev.primeclient.core.modules.prime;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.notification.NotificationManager;
import dev.primeclient.core.profile.ProfileManager;
import dev.primeclient.core.profile.ServerProfileBindings;

/**
 * Binds the active config profile to the current multiplayer server and
 * restores it automatically on the next join.
 */
public final class ServerAutoProfileModule extends Module {

    private final BooleanSetting autoSwitch = addSetting(new BooleanSetting(
            "auto-switch", "Auto switch", "Apply the bound profile when joining a server", true));
    private final BooleanSetting rememberOnLeave = addSetting(new BooleanSetting(
            "remember-leave", "Remember on leave", "Bind the active profile when leaving a server", false));

    private final MinecraftAdapter adapter;
    private final ProfileManager profiles;
    private final ServerProfileBindings bindings;
    private final NotificationManager notifications;

    public ServerAutoProfileModule(MinecraftAdapter adapter, ProfileManager profiles,
                                   ServerProfileBindings bindings, NotificationManager notifications) {
        super("server-auto-profile", "Server Auto Profile",
                "Switch config profiles per multiplayer server", ModuleCategory.PRIME);
        this.adapter = adapter;
        this.profiles = profiles;
        this.bindings = bindings;
        this.notifications = notifications;
    }

    /** Called from PrimeClient.onWorldJoin when this module is enabled. */
    public void onWorldJoin() {
        if (!isEnabled() || !autoSwitch.get()) {
            return;
        }
        String server = adapter.serverAddress();
        String bound = bindings.profileForServer(server);
        if (bound == null || bound.isBlank() || bound.equals(profiles.activeProfile())) {
            return;
        }
        try {
            profiles.switchTo(bound);
            notifications.info("Server Profile", "Switched to " + bound + " for " + server);
        } catch (IllegalArgumentException e) {
            notifications.info("Server Profile", "Bound profile missing: " + bound);
        }
    }

    /** Called from PrimeClient.onWorldLeave when remember-on-leave is on. */
    public void onWorldLeave() {
        if (!isEnabled() || !rememberOnLeave.get()) {
            return;
        }
        String server = adapter.serverAddress();
        if (server == null || server.isBlank()) {
            return;
        }
        bindings.bind(server, profiles.activeProfile());
    }

    /** Manual bind: current server → active profile. */
    public void bindCurrentServer() {
        String server = adapter.serverAddress();
        if (server == null || server.isBlank()) {
            notifications.info("Server Profile", "Join a multiplayer server first");
            return;
        }
        bindings.bind(server, profiles.activeProfile());
        notifications.info("Server Profile", server + " → " + profiles.activeProfile());
    }
}
