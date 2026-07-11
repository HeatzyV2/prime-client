package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.cloud.CloudClient;
import dev.primeclient.core.cloud.CloudSyncManager;
import dev.primeclient.core.profile.ProfileManager;
import dev.primeclient.core.theme.Theme;

/** Cloud config versions and sync controls. */
public final class ConfigurationsMenuRenderer {

    public void render(RenderContext ctx, Theme theme, CloudSyncManager cloud, ProfileManager profiles,
                       int screenW, int screenH) {
        int w = 300;
        int h = 180;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        ctx.fillRect(x, y, w, h, theme.background());
        ctx.fillRect(x, y, w, 2, theme.accent());
        ctx.drawText("Configurations", x + 12, y + 10, theme.accent(), true);
        ctx.drawText("Profile: " + profiles.activeProfile(), x + 12, y + 30, theme.foreground(), true);
        ctx.drawText("Auto sync: " + (cloud.autoSync() ? "ON" : "OFF"), x + 12, y + 46, theme.foregroundMuted(), true);

        int rowY = y + 66;
        var versions = cloud.listVersions(profiles.activeProfile());
        ctx.drawText("Versions (" + versions.size() + "):", x + 12, rowY, theme.foreground(), true);
        rowY += 14;
        int shown = 0;
        for (CloudClient.VersionEntry entry : versions) {
            if (shown++ >= 5) {
                break;
            }
            ctx.drawText("• " + entry.label(), x + 16, rowY, theme.foregroundMuted(), true);
            rowY += 12;
        }
        ctx.drawText("U=upload  D=download  Modules → Prime Config Cloud", x + 12, y + h - 16, theme.foregroundMuted(), true);
    }

    public boolean keyPressed(int key, CloudSyncManager cloud, ProfileManager profiles) {
        if (key == 85) { // U
            cloud.uploadNow(profiles.activeProfile());
            return true;
        }
        if (key == 68) { // D
            cloud.downloadNow(profiles.activeProfile());
            return true;
        }
        return false;
    }
}
