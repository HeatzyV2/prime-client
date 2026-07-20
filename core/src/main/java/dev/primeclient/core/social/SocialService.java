package dev.primeclient.core.social;

import java.util.concurrent.CompletableFuture;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.notification.NotificationManager;

/**
 * In-game social hub — connects to the unified Prime backend on world join.
 * Shares the same friend graph / DMs / party as the launcher.
 */
public final class SocialService {

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private final SocialSettings settings = new SocialSettings();
    private final SocialClient client = new SocialClient(settings);

    private volatile String pendingPartyServer;
    private int reconnectCooldown;
    private boolean wantConnected;

    public SocialService(MinecraftAdapter adapter, NotificationManager notifications) {
        this.adapter = adapter;
        this.notifications = notifications;
        client.onEvent(event -> {
            String t = event.has("t") ? event.get("t").getAsString() : "";
            if ("friend_request".equals(t)) {
                String name = event.has("fromUsername") ? event.get("fromUsername").getAsString() : "Player";
                notifications.info("Friend request", name + " wants to be friends");
            } else if ("message".equals(t) && event.has("message")) {
                var m = event.getAsJsonObject("message");
                String from = m.has("senderUsername") ? m.get("senderUsername").getAsString() : "Friend";
                String text = m.has("text") ? m.get("text").getAsString() : "";
                if (!text.isBlank()) {
                    notifications.info(from, text.length() > 48 ? text.substring(0, 48) + "…" : text);
                } else if (m.has("imageUrl") && !m.get("imageUrl").isJsonNull()) {
                    notifications.info(from, "[image]");
                }
            } else if ("party_invite".equals(t)) {
                String name = event.has("fromUsername") ? event.get("fromUsername").getAsString() : "Player";
                notifications.info("Party", name + " invited you");
            } else if ("party_join_server".equals(t) && event.has("serverAddress")) {
                String addr = event.get("serverAddress").getAsString();
                pendingPartyServer = addr;
                notifications.info("Party",
                        "Party wants you to join " + addr + " — open Social Hub or press Join");
            } else if ("party".equals(t) && event.has("party") && !event.get("party").isJsonNull()) {
                var partyObj = event.getAsJsonObject("party");
                if (partyObj.has("serverAddress") && !partyObj.get("serverAddress").isJsonNull()) {
                    String addr = partyObj.get("serverAddress").getAsString();
                    if (addr != null && !addr.isBlank()) {
                        pendingPartyServer = addr;
                    }
                }
            }
        });
    }

    public SocialSettings settings() {
        return settings;
    }

    public SocialClient client() {
        return client;
    }

    /** Pending party server address from a join prompt, if any. */
    public String pendingPartyServer() {
        return pendingPartyServer;
    }

    /** Clears and returns the pending party server address. */
    public String consumePendingPartyServer() {
        String addr = pendingPartyServer;
        pendingPartyServer = null;
        return addr;
    }

    public void ensureConnected() {
        wantConnected = true;
        if (!settings.enabled()) {
            return;
        }
        String uuid = adapter.playerUuid();
        String name = adapter.playerName();
        if (uuid == null || uuid.isBlank()) {
            return;
        }
        boolean offline = uuid.startsWith("00000000")
                || "offline".equalsIgnoreCase(adapter.sessionAccountType());
        client.connectAsync(uuid, name != null && !name.isBlank() ? name : "Player", offline);
    }

    public void onWorldJoin() {
        wantConnected = true;
        reconnectCooldown = 0;
        ensureConnected();
        String server = adapter.serverAddress();
        if (server != null && !server.isBlank() && !"Singleplayer".equalsIgnoreCase(server)) {
            client.setPresence("in-game", "Playing " + server, server);
            CompletableFuture.runAsync(() -> {
                for (int i = 0; i < 20; i++) {
                    if (client.connected()) {
                        client.setPartyServer(server);
                        return;
                    }
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        } else {
            client.setPresence("in-game", "In game", null);
        }
    }

    public void onWorldLeave() {
        wantConnected = false;
        client.setPresence("online", "In launcher", null);
        client.disconnect();
        pendingPartyServer = null;
        reconnectCooldown = 0;
    }

    public void tick() {
        if (!wantConnected || !settings.enabled()) {
            return;
        }
        if (client.connected()) {
            reconnectCooldown = 0;
            return;
        }
        if (client.state() == SocialClient.ConnState.CONNECTING) {
            return;
        }
        if (reconnectCooldown > 0) {
            reconnectCooldown--;
            return;
        }
        reconnectCooldown = 100; // ~5s at 20 TPS
        ensureConnected();
    }

    /** Uploads a crash report when the social client is authenticated. */
    public boolean uploadCrash(String title, String text) {
        return client.uploadCrash(title, text);
    }
}
