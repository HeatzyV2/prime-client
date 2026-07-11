package dev.primeclient.core.discord.ipc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.discord.DiscordPresenceSnapshot;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal Discord Rich Presence IPC client (pure Java, no native deps).
 *
 * <p>Windows: named pipes. Linux/macOS: Unix domain sockets in {@code $XDG_RUNTIME_DIR}.</p>
 */
public final class DiscordIpcClient {

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;

    private final String applicationId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Prime-Discord-RPC");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean connected = new AtomicBoolean();
    private volatile Object transport;
    private volatile long lastConnectAttempt;

    public DiscordIpcClient(String applicationId) {
        this.applicationId = applicationId;
    }

    public void connectAsync() {
        executor.execute(this::connectInternal);
    }

    public void updateAsync(DiscordPresenceSnapshot snapshot) {
        executor.execute(() -> {
            if (!ensureConnected()) {
                return;
            }
            sendFrame(buildSetActivity(snapshot));
        });
    }

    public void clearAsync() {
        executor.execute(() -> {
            if (!connected.get()) {
                return;
            }
            sendFrame(buildClearActivity());
        });
    }

    public void shutdown() {
        executor.execute(() -> {
            if (connected.get()) {
                sendFrame(buildClearActivity());
                closeTransport();
            }
            connected.set(false);
        });
        executor.shutdown();
    }

    private boolean ensureConnected() {
        if (connected.get()) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - lastConnectAttempt < 5000L) {
            return false;
        }
        return connectInternal();
    }

    private boolean connectInternal() {
        lastConnectAttempt = System.currentTimeMillis();
        closeTransport();
        for (int i = 0; i < 10; i++) {
            try {
                if (tryConnectWindows(i) || tryConnectUnix(i)) {
                    handshake();
                    connected.set(true);
                    PrimeClient.LOGGER.info("Discord Rich Presence connected (pipe {})", i);
                    return true;
                }
            } catch (IOException e) {
                closeTransport();
            }
        }
        connected.set(false);
        return false;
    }

    private boolean tryConnectWindows(int index) throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return false;
        }
        RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + index, "rw");
        transport = pipe;
        return true;
    }

    private boolean tryConnectUnix(int index) throws IOException {
        String base = System.getenv("XDG_RUNTIME_DIR");
        if (base == null || base.isBlank()) {
            base = "/tmp";
        }
        Path socketPath = Path.of(base, "discord-ipc-" + index);
        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        channel.connect(UnixDomainSocketAddress.of(socketPath));
        transport = channel;
        return true;
    }

    private void handshake() throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("v", 1);
        payload.addProperty("client_id", applicationId);
        writePacket(OP_HANDSHAKE, payload.toString());
        readPacket();
    }

    private void sendFrame(String json) {
        try {
            writePacket(OP_FRAME, json);
            readPacket();
        } catch (IOException e) {
            PrimeClient.LOGGER.debug("Discord IPC frame failed: {}", e.getMessage());
            connected.set(false);
            closeTransport();
        }
    }

    private String buildSetActivity(DiscordPresenceSnapshot snapshot) {
        JsonObject activity = new JsonObject();
        activity.addProperty("type", 0);
        if (!snapshot.details().isEmpty()) {
            activity.addProperty("details", trim(snapshot.details(), 128));
        }
        if (!snapshot.state().isEmpty()) {
            activity.addProperty("state", trim(snapshot.state(), 128));
        }
        if (snapshot.startEpochSeconds() != null) {
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", snapshot.startEpochSeconds());
            activity.add("timestamps", timestamps);
        }
        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", snapshot.largeImageKey());
        assets.addProperty("large_text", trim(snapshot.largeImageText(), 128));
        assets.addProperty("small_image", snapshot.smallImageKey());
        if (!snapshot.smallImageText().isEmpty()) {
            assets.addProperty("small_text", trim(snapshot.smallImageText(), 128));
        }
        activity.add("assets", assets);
        if (!snapshot.buttons().isEmpty()) {
            JsonArray buttons = new JsonArray();
            JsonArray labels = new JsonArray();
            for (DiscordPresenceSnapshot.Button button : snapshot.buttons()) {
                buttons.add(button.url());
                labels.add(trim(button.label(), 32));
            }
            activity.add("buttons", buttons);
            activity.add("metadata", metadataWithLabels(labels));
        }

        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", activity);

        JsonObject root = new JsonObject();
        root.addProperty("cmd", "SET_ACTIVITY");
        root.addProperty("nonce", Long.toString(System.nanoTime()));
        root.add("args", args);
        return root.toString();
    }

    private static JsonObject metadataWithLabels(JsonArray labels) {
        JsonObject metadata = new JsonObject();
        metadata.add("button_label", labels);
        return metadata;
    }

    private String buildClearActivity() {
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", com.google.gson.JsonNull.INSTANCE);

        JsonObject root = new JsonObject();
        root.addProperty("cmd", "SET_ACTIVITY");
        root.addProperty("nonce", Long.toString(System.nanoTime()));
        root.add("args", args);
        return root.toString();
    }

    private void writePacket(int opcode, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(data.length);
        header.flip();
        writeBytes(header.array());
        writeBytes(data);
    }

    private void writeBytes(byte[] bytes) throws IOException {
        Object current = transport;
        if (current instanceof RandomAccessFile pipe) {
            pipe.write(bytes);
        } else if (current instanceof SocketChannel channel) {
            channel.write(ByteBuffer.wrap(bytes));
        } else {
            throw new IOException("No Discord IPC transport");
        }
    }

    private void readPacket() throws IOException {
        byte[] header = readBytes(8);
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = buffer.getInt();
        int length = buffer.getInt();
        if (length > 0) {
            readBytes(length);
        }
        if (opcode == OP_CLOSE) {
            throw new IOException("Discord IPC closed connection");
        }
    }

    private byte[] readBytes(int length) throws IOException {
        byte[] out = new byte[length];
        Object current = transport;
        if (current instanceof RandomAccessFile pipe) {
            pipe.readFully(out);
        } else if (current instanceof SocketChannel channel) {
            ByteBuffer buffer = ByteBuffer.wrap(out);
            while (buffer.hasRemaining()) {
                int read = channel.read(buffer);
                if (read < 0) {
                    throw new IOException("Discord IPC EOF");
                }
            }
        } else {
            throw new IOException("No Discord IPC transport");
        }
        return out;
    }

    private void closeTransport() {
        Object current = transport;
        transport = null;
        if (current instanceof RandomAccessFile pipe) {
            try {
                pipe.close();
            } catch (IOException ignored) {
            }
        } else if (current instanceof SocketChannel channel) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String trim(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(0, max - 1)) + "…";
    }
}
