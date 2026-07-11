package dev.primeclient.core.modules.prime;

import dev.primeclient.core.cloud.CloudSyncManager;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.profile.ProfileManager;

/** Cloud sync with versioned backups (local stub, API-ready). */
public final class PrimeConfigCloudModule extends Module {

    private final BooleanSetting autoSync =
            addSetting(new BooleanSetting("auto-sync", "Auto sync", "Upload on profile change", false));
    private final BooleanSetting uploadNow =
            addSetting(new BooleanSetting("upload", "Upload now", "Upload on next tick", false));
    private final BooleanSetting downloadNow =
            addSetting(new BooleanSetting("download", "Download now", "Download on next tick", false));

    private final CloudSyncManager cloudSync;
    private final ProfileManager profiles;

    public PrimeConfigCloudModule(CloudSyncManager cloudSync, ProfileManager profiles) {
        super("prime-config-cloud", "Prime Config Cloud", "Sync configs with Prime Cloud", ModuleCategory.PRIME);
        this.cloudSync = cloudSync;
        this.profiles = profiles;
        listen(ClientTickEvent.class, event -> process());
    }

    @Override
    protected void onEnable() {
        cloudSync.setAutoSync(autoSync.get());
        cloudSync.uploadNow(profiles.activeProfile());
    }

    @Override
    protected void onDisable() {
        cloudSync.setAutoSync(false);
    }

    private void process() {
        cloudSync.setAutoSync(autoSync.get() && isEnabled());
        if (uploadNow.get()) {
            uploadNow.set(false);
            cloudSync.uploadNow(profiles.activeProfile());
        }
        if (downloadNow.get()) {
            downloadNow.set(false);
            cloudSync.downloadNow(profiles.activeProfile());
        }
    }
}
