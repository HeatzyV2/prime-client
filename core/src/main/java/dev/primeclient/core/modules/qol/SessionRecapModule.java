package dev.primeclient.core.modules.qol;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.PlayerDeathEvent;
import dev.primeclient.core.event.WorldLeaveEvent;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.notification.NotificationManager;

/** Session recap on disconnect or world leave. */
public final class SessionRecapModule extends Module {

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;

    private int deaths;
    private int pearlsThrown;
    private long sessionStart;

    public SessionRecapModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("session-recap", "Session Recap", "Summary when leaving a world", ModuleCategory.QOL);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(PlayerDeathEvent.class, event -> deaths++);
        listen(WorldLeaveEvent.class, event -> showRecap());
    }

    @Override
    protected void onEnable() {
        deaths = 0;
        pearlsThrown = 0;
        sessionStart = System.currentTimeMillis();
    }

    /** Called from pearl cooldown tracking via adapter heuristic on disable. */
    public void trackPearlThrow() {
        pearlsThrown++;
    }

    private void showRecap() {
        long minutes = Math.max(1, (System.currentTimeMillis() - sessionStart) / 60_000L);
        notifications.info("Session Recap",
                minutes + " min · " + deaths + " deaths · ~" + pearlsThrown + " pearls");
    }
}
