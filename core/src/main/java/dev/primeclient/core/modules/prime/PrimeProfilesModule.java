package dev.primeclient.core.modules.prime;

import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.module.StringSetting;
import dev.primeclient.core.profile.ProfileManager;

/**
 * Switches between named configuration profiles.
 *
 * <p>The profile setting accepts any name from {@link ProfileManager#listProfiles()},
 * including custom profiles created in Settings.</p>
 */
public final class PrimeProfilesModule extends Module {

    private final StringSetting profile =
            addSetting(new StringSetting("profile", "Profile",
                    "Active configuration profile name (default, pvp, survival, or custom)",
                    ProfileManager.DEFAULT_PROFILE));

    private final ProfileManager profiles;
    private String lastApplied = "";

    public PrimeProfilesModule(ProfileManager profiles) {
        super("prime-profiles", "Prime Profiles",
                "Switch between named configuration profiles", ModuleCategory.PRIME);
        this.profiles = profiles;

        listen(ClientTickEvent.class, event -> applyProfile());
    }

    @Override
    protected void onEnable() {
        syncFromActive();
    }

    private void syncFromActive() {
        String active = profiles.activeProfile();
        profile.set(active);
        lastApplied = active;
    }

    private void applyProfile() {
        String active = profiles.activeProfile();
        String target = profile.get() == null ? "" : profile.get().trim();
        if (target.isEmpty()) {
            target = ProfileManager.DEFAULT_PROFILE;
            profile.set(target);
        }

        // External switch (radial, settings, server auto-profile) — mirror into the setting.
        if (target.equals(lastApplied) && !target.equals(active)) {
            profile.set(active);
            lastApplied = active;
            return;
        }

        if (target.equals(active)) {
            lastApplied = active;
            return;
        }

        try {
            profiles.switchTo(target);
            lastApplied = target;
        } catch (IllegalArgumentException ex) {
            profile.set(active);
            lastApplied = active;
        }
    }
}
