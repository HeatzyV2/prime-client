package dev.primeclient.core.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.primeclient.core.crash.CrashReportUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * HTTP + WebSocket client for the unified Prime social backend.
 * Syncs with launcher: same API, same account UUID.
 */
public final class SocialClient implements WebSocket.Listener {

    public enum ConnState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("Prime Social");

    public record Friend(String uuid, String username, String status, String activity, String serverAddress,
                         boolean pending, boolean incoming) {}

    public record ChatMessage(String id, String conversationId, String senderUuid, String senderUsername,
                              String text, String imageUrl, String createdAt) {}

    public record PartyMember(String uuid, String username, boolean leader) {}

    public record Party(String id, String leaderUuid, String serverAddress, List<PartyMember> members) {}

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .proxy(HttpClient.Builder.NO_PROXY)
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private final SocialSettings settings;
    private final List<Consumer<JsonObject>> eventListeners = new CopyOnWriteArrayList<>();
    private final Map<String, Friend> friends = new ConcurrentHashMap<>();
    private final List<ChatMessage> recentMessages = new CopyOnWriteArrayList<>();
    private final AtomicBoolean connectInFlight = new AtomicBoolean(false);

    private volatile String token;
    private volatile String selfUuid;
    private volatile WebSocket socket;
    private volatile Party party;
    private volatile ConnState state = ConnState.DISCONNECTED;
    private volatile String statusMessage = "";
    private volatile String pendingPresenceStatus;
    private volatile String pendingPresenceActivity;
    private volatile String pendingPresenceServer;
    private volatile String pendingPartyServerPublish;
    private final StringBuilder textBuffer = new StringBuilder();

    public SocialClient(SocialSettings settings) {
        this.settings = settings;
    }

    public void onEvent(Consumer<JsonObject> listener) {
        eventListeners.add(listener);
    }

    public Map<String, Friend> friends() {
        return friends;
    }

    public List<ChatMessage> recentMessages() {
        return recentMessages;
    }

    public Party party() {
        return party;
    }

    public String selfUuid() {
        return selfUuid;
    }

    public ConnState state() {
        return state;
    }

    public String statusMessage() {
        return statusMessage == null ? "" : statusMessage;
    }

    public boolean connected() {
        return state == ConnState.CONNECTED && token != null && socket != null;
    }

    public boolean uploadCrash(String title, String text) {
        return CrashReportUploader.upload(settings, title, text, token);
    }

    /** Non-blocking connect — safe to call from the Minecraft client thread. */
    public void connectAsync(String uuid, String username, boolean offline) {
        if (!settings.enabled()) {
            state = ConnState.DISCONNECTED;
            statusMessage = "Social disabled";
            return;
        }
        if (uuid == null || uuid.isBlank() || username == null || username.isBlank()) {
            state = ConnState.ERROR;
            statusMessage = "Missing account UUID";
            return;
        }
        if (connected() && uuid.equalsIgnoreCase(selfUuid)) {
            return;
        }
        if (!connectInFlight.compareAndSet(false, true)) {
            return;
        }
        state = ConnState.CONNECTING;
        statusMessage = "Connecting…";
        final String connectUuid = uuid.trim();
        final String connectName = username.trim();
        final boolean connectOffline = offline;
        CompletableFuture.runAsync(() -> {
            try {
                connectBlocking(connectUuid, connectName, connectOffline);
            } finally {
                connectInFlight.set(false);
            }
        });
    }

    /** @deprecated Prefer {@link #connectAsync} — kept for call-site compatibility. */
    @Deprecated
    public void connect(String uuid, String username, boolean offline) {
        connectAsync(uuid, username, offline);
    }

    private void connectBlocking(String uuid, String username, boolean offline) {
        disconnectQuiet();
        this.selfUuid = uuid;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("uuid", uuid);
            body.addProperty("username", username);
            body.addProperty("offline", offline);
            body.addProperty("client", "game");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(settings.apiBase() + "/v1/auth/session"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PrimeClient-Game")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) {
                state = ConnState.ERROR;
                statusMessage = "Auth failed (HTTP " + res.statusCode() + ")";
                LOGGER.warn("Social auth failed: HTTP {} body={}", res.statusCode(), truncate(res.body()));
                return;
            }
            JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
            token = json.get("token").getAsString();
            statusMessage = "Opening socket…";
            refreshFriendsBlocking();
            openSocket();
        } catch (Exception e) {
            state = ConnState.ERROR;
            statusMessage = "Connect failed: " + shortMsg(e);
            LOGGER.warn("Social connect failed: {}", e.toString());
        }
    }

    public void disconnect() {
        connectInFlight.set(false);
        disconnectQuiet();
        state = ConnState.DISCONNECTED;
        statusMessage = "Disconnected";
    }

    private void disconnectQuiet() {
        token = null;
        try {
            WebSocket ws = socket;
            socket = null;
            if (ws != null) {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "leave");
            }
        } catch (Exception ignored) {
        }
        friends.clear();
        party = null;
    }

    public void setPresence(String status, String activity, String serverAddress) {
        pendingPresenceStatus = status;
        pendingPresenceActivity = activity;
        pendingPresenceServer = serverAddress;
        flushPresence();
    }

    private void flushPresence() {
        WebSocket ws = socket;
        if (ws == null || token == null || pendingPresenceStatus == null) {
            return;
        }
        JsonObject msg = new JsonObject();
        msg.addProperty("t", "presence");
        msg.addProperty("status", pendingPresenceStatus);
        msg.addProperty("activity", pendingPresenceActivity == null ? "" : pendingPresenceActivity);
        if (pendingPresenceServer != null) {
            msg.addProperty("serverAddress", pendingPresenceServer);
        }
        msg.addProperty("client", "game");
        try {
            ws.sendText(msg.toString(), true);
        } catch (Exception e) {
            LOGGER.debug("Presence send failed: {}", e.getMessage());
        }
    }

    public void refreshFriends() {
        if (token == null) {
            return;
        }
        CompletableFuture.runAsync(this::refreshFriendsBlocking);
    }

    private void refreshFriendsBlocking() {
        if (token == null) {
            return;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(settings.apiBase() + "/v1/friends"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "PrimeClient-Game")
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) {
                return;
            }
            JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
            friends.clear();
            JsonArray arr = json.getAsJsonArray("friends");
            if (arr == null) {
                return;
            }
            for (JsonElement el : arr) {
                JsonObject f = el.getAsJsonObject();
                String uuid = f.get("uuid").getAsString();
                JsonObject presence = f.has("presence") ? f.getAsJsonObject("presence") : new JsonObject();
                String status = f.get("status").getAsString();
                friends.put(uuid, new Friend(
                        uuid,
                        f.get("username").getAsString(),
                        presence.has("status") ? presence.get("status").getAsString() : "offline",
                        presence.has("activity") ? presence.get("activity").getAsString() : "",
                        presence.has("serverAddress") && !presence.get("serverAddress").isJsonNull()
                                ? presence.get("serverAddress").getAsString() : null,
                        "pending".equals(status),
                        f.has("incoming") && f.get("incoming").getAsBoolean()
                ));
            }
        } catch (Exception e) {
            LOGGER.debug("Friends refresh failed: {}", e.getMessage());
        }
    }

    public boolean requestFriend(String username) {
        return postJson("/v1/friends/request", obj -> obj.addProperty("username", username));
    }

    public boolean acceptFriend(String uuid) {
        return postJson("/v1/friends/accept", obj -> obj.addProperty("uuid", uuid));
    }

    public String openDm(String uuid) {
        if (token == null) {
            return null;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("uuid", uuid);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(settings.apiBase() + "/v1/conversations/dm"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) {
                return null;
            }
            return JsonParser.parseString(res.body()).getAsJsonObject()
                    .getAsJsonObject("conversation").get("id").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean sendMessage(String conversationId, String text) {
        if (token == null) {
            return false;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("text", text);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(settings.apiBase() + "/v1/conversations/" + conversationId + "/messages"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            return http.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() / 100 == 2;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean createParty() {
        return postJson("/v1/party", obj -> {});
    }

    public boolean inviteParty(String uuid) {
        return postJson("/v1/party/invite", obj -> obj.addProperty("uuid", uuid));
    }

    public boolean leaveParty() {
        return postJson("/v1/party/leave", obj -> {});
    }

    public boolean setPartyServer(String serverAddress) {
        return postJson("/v1/party/server", obj -> obj.addProperty("serverAddress", serverAddress));
    }

    /** Queues a party-server publish until the socket is connected (and party exists). */
    public void queuePartyServerPublish(String serverAddress) {
        if (serverAddress == null || serverAddress.isBlank()) {
            return;
        }
        pendingPartyServerPublish = serverAddress.trim();
        flushPendingPartyServer();
    }

    public void clearPendingPartyServerPublish() {
        pendingPartyServerPublish = null;
    }

    private void flushPendingPartyServer() {
        String server = pendingPartyServerPublish;
        if (server == null || !connected() || party == null) {
            return;
        }
        // Leader-only on the backend — non-leaders fail silently and keep the queue.
        if (setPartyServer(server)) {
            pendingPartyServerPublish = null;
        }
    }

    public boolean refreshParty() {
        if (token == null) {
            return false;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(settings.apiBase() + "/v1/party"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "PrimeClient-Game")
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) {
                return false;
            }
            JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
            if (json.has("party") && !json.get("party").isJsonNull()) {
                party = parseParty(json.getAsJsonObject("party"));
            } else {
                party = null;
            }
            flushPendingPartyServer();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean postJson(String path, Consumer<JsonObject> bodyBuilder) {
        if (token == null) {
            return false;
        }
        try {
            JsonObject body = new JsonObject();
            bodyBuilder.accept(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(settings.apiBase() + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PrimeClient-Game")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            return http.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() / 100 == 2;
        } catch (Exception e) {
            return false;
        }
    }

    private void openSocket() {
        URI uri = URI.create(settings.socialWsUrl(token));
        http.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .header("User-Agent", "PrimeClient-Game")
                .buildAsync(uri, this)
                .whenComplete((ws, err) -> {
                    if (err != null) {
                        state = ConnState.ERROR;
                        statusMessage = "WS failed: " + shortMsg(err);
                        LOGGER.warn("Social WS failed: {}", err.toString());
                        return;
                    }
                    socket = ws;
                    state = ConnState.CONNECTED;
                    statusMessage = "Connected";
                    if (pendingPresenceStatus == null) {
                        pendingPresenceStatus = "in-game";
                        pendingPresenceActivity = "In game";
                    }
                    flushPresence();
                    CompletableFuture.runAsync(() -> {
                        refreshParty();
                        flushPendingPartyServer();
                    });
                });
    }

    private void emit(JsonObject msg) {
        for (Consumer<JsonObject> listener : eventListeners) {
            try {
                listener.accept(msg);
            } catch (Exception ignored) {
            }
        }
    }

    private void handleEvent(JsonObject msg) {
        String t = msg.has("t") ? msg.get("t").getAsString() : "";
        if ("ready".equals(t)) {
            state = ConnState.CONNECTED;
            statusMessage = "Connected";
        }
        if ("snapshot".equals(t) && msg.has("friends")) {
            friends.clear();
            for (JsonElement el : msg.getAsJsonArray("friends")) {
                JsonObject f = el.getAsJsonObject();
                String uuid = f.get("uuid").getAsString();
                friends.put(uuid, new Friend(
                        uuid,
                        f.get("username").getAsString(),
                        f.has("status") ? f.get("status").getAsString() : "offline",
                        f.has("activity") ? f.get("activity").getAsString() : "",
                        f.has("serverAddress") && !f.get("serverAddress").isJsonNull()
                                ? f.get("serverAddress").getAsString() : null,
                        false,
                        false
                ));
            }
            if (msg.has("party") && !msg.get("party").isJsonNull()) {
                party = parseParty(msg.getAsJsonObject("party"));
            }
        }
        if ("presence".equals(t) && msg.has("uuid")) {
            String uuid = msg.get("uuid").getAsString();
            Friend prev = friends.get(uuid);
            friends.put(uuid, new Friend(
                    uuid,
                    msg.has("username") ? msg.get("username").getAsString() : (prev != null ? prev.username() : "Player"),
                    msg.has("status") ? msg.get("status").getAsString() : "offline",
                    msg.has("activity") ? msg.get("activity").getAsString() : "",
                    msg.has("serverAddress") && !msg.get("serverAddress").isJsonNull()
                            ? msg.get("serverAddress").getAsString() : null,
                    prev != null && prev.pending(),
                    prev != null && prev.incoming()
            ));
        }
        if ("message".equals(t) && msg.has("message")) {
            JsonObject m = msg.getAsJsonObject("message");
            ChatMessage chat = new ChatMessage(
                    m.get("id").getAsString(),
                    m.get("conversationId").getAsString(),
                    m.get("senderUuid").getAsString(),
                    m.has("senderUsername") ? m.get("senderUsername").getAsString() : "",
                    m.has("text") ? m.get("text").getAsString() : "",
                    m.has("imageUrl") && !m.get("imageUrl").isJsonNull() ? m.get("imageUrl").getAsString() : null,
                    m.get("createdAt").getAsString()
            );
            recentMessages.add(chat);
            if (recentMessages.size() > 100) {
                recentMessages.remove(0);
            }
        }
        if ("party".equals(t)) {
            party = msg.has("party") && !msg.get("party").isJsonNull()
                    ? parseParty(msg.getAsJsonObject("party")) : null;
        }
        emit(msg);
    }

    private Party parseParty(JsonObject p) {
        List<PartyMember> members = new ArrayList<>();
        if (p.has("members")) {
            for (JsonElement el : p.getAsJsonArray("members")) {
                JsonObject m = el.getAsJsonObject();
                members.add(new PartyMember(
                        m.get("uuid").getAsString(),
                        m.get("username").getAsString(),
                        m.has("leader") && m.get("leader").getAsBoolean()
                ));
            }
        } else if (p.has("memberUuids")) {
            for (JsonElement el : p.getAsJsonArray("memberUuids")) {
                members.add(new PartyMember(el.getAsString(), el.getAsString(), false));
            }
        }
        return new Party(
                p.get("id").getAsString(),
                p.get("leaderUuid").getAsString(),
                p.has("serverAddress") && !p.get("serverAddress").isJsonNull()
                        ? p.get("serverAddress").getAsString() : null,
                members
        );
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        textBuffer.append(data);
        if (last) {
            try {
                handleEvent(JsonParser.parseString(textBuffer.toString()).getAsJsonObject());
            } catch (Exception ignored) {
            }
            textBuffer.setLength(0);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (socket == webSocket) {
            socket = null;
            if (state == ConnState.CONNECTED) {
                state = ConnState.DISCONNECTED;
                statusMessage = "Disconnected";
            }
        }
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        if (socket == webSocket) {
            socket = null;
        }
        state = ConnState.ERROR;
        statusMessage = "WS error: " + shortMsg(error);
        LOGGER.warn("Social WS error: {}", error.toString());
    }

    private static String shortMsg(Throwable e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) {
            m = e.getClass().getSimpleName();
        }
        return m.length() > 64 ? m.substring(0, 64) + "…" : m;
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }
}
