package dev.primeclient.core.discord;

import dev.primeclient.core.account.PrimeAccountService;
import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.EventBus;
import dev.primeclient.core.keybind.KeybindManager;
import dev.primeclient.core.module.ModuleManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordRpcServiceTest {

    private static final class StubAdapter implements MinecraftAdapter {
        String server = "play.hypixel.net";
        String player = "Zorat";
        boolean inGame = true;

        @Override
        public String minecraftVersion() {
            return "26.2";
        }

        @Override
        public Path gameDirectory() {
            return Path.of(".");
        }

        @Override
        public Path configDirectory() {
            return Path.of(".");
        }

        @Override
        public boolean isInGame() {
            return inGame;
        }

        @Override
        public boolean isScreenOpen() {
            return false;
        }

        @Override
        public boolean isKeyDown(int glfwKey) {
            return false;
        }

        @Override
        public boolean isMouseButtonDown(int glfwButton) {
            return false;
        }

        @Override
        public int fps() {
            return 144;
        }

        @Override
        public boolean hasPlayer() {
            return inGame;
        }

        @Override
        public double playerX() {
            return 120.5;
        }

        @Override
        public double playerY() {
            return 64;
        }

        @Override
        public double playerZ() {
            return -45.2;
        }

        @Override
        public void runOnClientThread(Runnable task) {
            task.run();
        }

        @Override
        public void openHudEditor() {
        }

        @Override
        public void openClickGui() {
        }

        @Override
        public int ping() {
            return 38;
        }

        @Override
        public String serverAddress() {
            return server;
        }

        @Override
        public String playerName() {
            return player;
        }

        @Override
        public float playerHealth() {
            return 17;
        }

        @Override
        public String biomeName() {
            return "Plains";
        }

        @Override
        public String heldItemName() {
            return "Diamond Sword";
        }
    }

    @Test
    void multiplayerSnapshotIncludesServerAndPlayer() {
        DiscordRpcService service = new DiscordRpcService();
        StubAdapter adapter = new StubAdapter();
        ModuleManager modules = new ModuleManager(new EventBus(), new KeybindManager());
        PrimeAccountService account = new PrimeAccountService();
        account.login("Zorat");

        DiscordPresenceSnapshot snapshot = service.buildSnapshot(adapter, modules, account);

        assertTrue(snapshot.details().contains("play.hypixel.net"));
        assertTrue(snapshot.state().contains("Zorat"));
        assertTrue(snapshot.state().contains("38ms"));
        assertTrue(snapshot.state().contains("Plains"));
        assertTrue(snapshot.buttons().size() >= 2);
    }

    @Test
    void menuSnapshotWhenNotInGame() {
        DiscordRpcService service = new DiscordRpcService();
        StubAdapter adapter = new StubAdapter();
        adapter.inGame = false;
        ModuleManager modules = new ModuleManager(new EventBus(), new KeybindManager());

        DiscordPresenceSnapshot snapshot = service.buildSnapshot(adapter, modules, new PrimeAccountService());

        assertTrue(snapshot.details().contains("Main Menu"));
        assertTrue(snapshot.state().contains("26.2"));
    }
}
