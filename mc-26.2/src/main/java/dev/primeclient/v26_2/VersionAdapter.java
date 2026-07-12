package dev.primeclient.v26_2;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.gui.menu.TitleScreenGate;
import dev.primeclient.core.util.DirectionUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.particles.DustParticleOptions;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** {@link MinecraftAdapter} for Minecraft 26.2. */
public final class VersionAdapter implements MinecraftAdapter {

    private long sessionStartMillis;

    @Override
    public String minecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public Path gameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isInGame() {
        return Minecraft.getInstance().level != null;
    }

    @Override
    public boolean isScreenOpen() {
        return Minecraft.getInstance().gui.screen() != null;
    }

    @Override
    public boolean isKeyDown(int glfwKey) {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, glfwKey) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean isMouseButtonDown(int glfwButton) {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetMouseButton(window, glfwButton) == GLFW.GLFW_PRESS;
    }

    @Override
    public int fps() {
        return Minecraft.getInstance().getFps();
    }

    @Override
    public boolean hasPlayer() {
        return Minecraft.getInstance().player != null;
    }

    @Override
    public double playerX() {
        return Minecraft.getInstance().player.getX();
    }

    @Override
    public double playerY() {
        return Minecraft.getInstance().player.getY();
    }

    @Override
    public double playerZ() {
        return Minecraft.getInstance().player.getZ();
    }

    @Override
    public void runOnClientThread(Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    @Override
    public void openHudEditor() {
        Minecraft.getInstance().gui.setScreen(new dev.primeclient.v26_2.screen.HudEditorScreen());
    }

    @Override
    public void openClickGui() {
        Minecraft.getInstance().gui.setScreen(new dev.primeclient.v26_2.screen.ClickGuiScreen());
    }

    @Override
    public void openPrimeSettings() {
        PrimeClient.get().clickGui().showSettings();
        Minecraft.getInstance().gui.setScreen(new dev.primeclient.v26_2.screen.ClickGuiScreen());
    }

    @Override
    public void openVanillaTitleScreen() {
        TitleScreenGate.requestVanilla();
        Minecraft.getInstance().gui.setScreen(new TitleScreen());
    }

    @Override
    public void openExternalLink(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void openSingleplayer() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.gui.screen() != null ? mc.gui.screen() : null;
        mc.gui.setScreen(new SelectWorldScreen(parent));
    }

    @Override
    public void openMultiplayer() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.gui.screen() != null ? mc.gui.screen() : null;
        mc.gui.setScreen(new JoinMultiplayerScreen(parent));
    }

    @Override
    public void openOptions() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.gui.screen() != null ? mc.gui.screen() : null;
        mc.gui.setScreen(new OptionsScreen(parent, mc.options, false));
    }

    @Override
    public void quitGame() {
        Minecraft.getInstance().stop();
    }

    @Override
    public void closeCurrentScreen() {
        Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public int ping() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null) {
            return 0;
        }
        var info = player.connection.getPlayerInfo(player.getUUID());
        return info != null ? info.getLatency() : 0;
    }

    @Override
    public String serverAddress() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server != null ? server.ip : "Singleplayer";
    }

    @Override
    public float playerYaw() {
        return Minecraft.getInstance().player.getYRot();
    }

    @Override
    public float playerPitch() {
        return Minecraft.getInstance().player.getXRot();
    }

    @Override
    public String playerName() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return player.getName().getString();
        }
        var user = Minecraft.getInstance().getUser();
        return user != null ? user.getName() : "";
    }

    @Override
    public float playerHealth() {
        return Minecraft.getInstance().player.getHealth();
    }

    @Override
    public float playerMaxHealth() {
        return Minecraft.getInstance().player.getMaxHealth();
    }

    @Override
    public boolean isSprinting() {
        return Minecraft.getInstance().player.isSprinting();
    }

    @Override
    public void setSprinting(boolean sprint) {
        Minecraft.getInstance().player.setSprinting(sprint);
    }

    @Override
    public boolean isSneaking() {
        return Minecraft.getInstance().player.isShiftKeyDown();
    }

    @Override
    public void setSneaking(boolean sneak) {
        Minecraft.getInstance().player.setShiftKeyDown(sneak);
    }

    @Override
    public boolean isMovingForward() {
        return Minecraft.getInstance().options.keyUp.isDown();
    }

    @Override
    public boolean isDead() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && (player.isDeadOrDying() || Minecraft.getInstance().gui.screen() instanceof DeathScreen);
    }

    @Override
    public void respawn() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.respawn();
        }
    }

    @Override
    public boolean hasTarget() {
        return Minecraft.getInstance().crosshairPickEntity != null;
    }

    @Override
    public String targetName() {
        var entity = Minecraft.getInstance().crosshairPickEntity;
        return entity != null ? entity.getName().getString() : "";
    }

    @Override
    public float targetHealth() {
        var entity = Minecraft.getInstance().crosshairPickEntity;
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            return living.getHealth();
        }
        return 0;
    }

    @Override
    public float targetMaxHealth() {
        var entity = Minecraft.getInstance().crosshairPickEntity;
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            return living.getMaxHealth();
        }
        return 20;
    }

    @Override
    public float attackCooldown() {
        return Minecraft.getInstance().player.getAttackStrengthScale(0);
    }

    @Override
    public boolean hasArmor(int slot) {
        return !armorStack(slot).isEmpty();
    }

    @Override
    public int armorDurability(int slot) {
        ItemStack stack = armorStack(slot);
        return stack.isEmpty() ? 0 : stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public int armorMaxDurability(int slot) {
        ItemStack stack = armorStack(slot);
        return stack.isEmpty() ? 0 : stack.getMaxDamage();
    }

    @Override
    public int potionEffectCount() {
        return Minecraft.getInstance().player.getActiveEffects().size();
    }

    @Override
    public String potionName(int index) {
        MobEffectInstance effect = effectAt(index);
        return effect != null ? effect.getEffect().value().getDisplayName().getString() : "";
    }

    @Override
    public int potionDurationSeconds(int index) {
        MobEffectInstance effect = effectAt(index);
        return effect != null ? effect.getDuration() / 20 : 0;
    }

    @Override
    public int potionAmplifier(int index) {
        MobEffectInstance effect = effectAt(index);
        return effect != null ? effect.getAmplifier() : 0;
    }

    @Override
    public String heldItemName() {
        ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    @Override
    public int heldItemCount() {
        return Minecraft.getInstance().player.getMainHandItem().getCount();
    }

    @Override
    public String hoveredItemName() {
        ItemStack stack = Minecraft.getInstance().player.containerMenu.getCarried();
        if (!stack.isEmpty()) {
            return stack.getHoverName().getString();
        }
        return heldItemName();
    }

    @Override
    public boolean hoveredItemIsShulkerBox() {
        return Minecraft.getInstance().player.getMainHandItem().getItem().toString().contains("shulker_box");
    }

    @Override
    public int shulkerSlotCount() {
        ItemContainerContents contents = shulkerContents();
        if (contents == null) {
            return 0;
        }
        int count = 0;
        for (var slot : contents.nonEmptyItems()) {
            if (slot.count() > 0) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String shulkerSlotItem(int index) {
        ItemContainerContents contents = shulkerContents();
        if (contents == null) {
            return "";
        }
        int i = 0;
        for (var slot : contents.nonEmptyItems()) {
            if (slot.count() > 0) {
                if (i == index) {
                    return slot.item().value().getDescriptionId();
                }
                i++;
            }
        }
        return "";
    }

    @Override
    public int shulkerSlotCount(int index) {
        ItemContainerContents contents = shulkerContents();
        if (contents == null) {
            return 0;
        }
        int i = 0;
        for (var slot : contents.nonEmptyItems()) {
            if (slot.count() > 0) {
                if (i == index) {
                    return slot.count();
                }
                i++;
            }
        }
        return 0;
    }

    @Override
    public long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    @Override
    public long maxMemoryMb() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    @Override
    public void suggestGarbageCollection() {
        System.gc();
    }

    @Override
    public void sendChat(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.connection != null) {
            if (message.startsWith("/")) {
                player.connection.sendCommand(message.substring(1));
            } else {
                player.connection.sendChat(message);
            }
        }
    }

    @Override
    public long sessionMillis() {
        return sessionStartMillis == 0 ? 0 : System.currentTimeMillis() - sessionStartMillis;
    }

    public void markSessionStart() {
        sessionStartMillis = System.currentTimeMillis();
    }

    public void markSessionEnd() {
        sessionStartMillis = 0;
    }

    @Override
    public String facingDirection() {
        return DirectionUtil.fromYaw(playerYaw());
    }

    @Override
    public String biomeName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return "";
        }
        return mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
                .map(key -> key.identifier().getPath())
                .orElse("");
    }

    @Override
    public int renderDistance() {
        return Minecraft.getInstance().options.renderDistance().get();
    }

    @Override
    public void setRenderDistance(int chunks) {
        Minecraft.getInstance().options.renderDistance().set(chunks);
    }

    @Override
    public int simulationDistance() {
        return Minecraft.getInstance().options.simulationDistance().get();
    }

    @Override
    public void setSimulationDistance(int chunks) {
        Minecraft.getInstance().options.simulationDistance().set(chunks);
    }

    @Override
    public int maxFps() {
        return Minecraft.getInstance().options.framerateLimit().get();
    }

    @Override
    public void setMaxFps(int fps) {
        Minecraft.getInstance().options.framerateLimit().set(fps);
    }

    @Override
    public int particleSetting() {
        return Minecraft.getInstance().options.particles().get().ordinal();
    }

    @Override
    public void setParticleSetting(int setting) {
        ParticleStatus[] values = ParticleStatus.values();
        if (setting >= 0 && setting < values.length) {
            Minecraft.getInstance().options.particles().set(values[setting]);
        }
    }

    @Override
    public boolean cloudsEnabled() {
        return Minecraft.getInstance().options.cloudStatus().get() != net.minecraft.client.CloudStatus.OFF;
    }

    @Override
    public void setCloudsEnabled(boolean enabled) {
        Minecraft.getInstance().options.cloudStatus().set(
                enabled ? net.minecraft.client.CloudStatus.FANCY : net.minecraft.client.CloudStatus.OFF);
    }

    @Override
    public int entityDistance() {
        return (int) (Minecraft.getInstance().options.entityDistanceScaling().get() * 100);
    }

    @Override
    public void setEntityDistance(int percent) {
        Minecraft.getInstance().options.entityDistanceScaling().set(percent / 100.0);
    }

    @Override
    public boolean fancyGraphics() {
        GraphicsPreset preset = Minecraft.getInstance().options.graphicsPreset().get();
        return preset == GraphicsPreset.FANCY || preset == GraphicsPreset.FABULOUS;
    }

    @Override
    public void setFancyGraphics(boolean enabled) {
        Minecraft.getInstance().options.graphicsPreset().set(
                enabled ? GraphicsPreset.FANCY : GraphicsPreset.FAST);
    }

    @Override
    public boolean isWindowFocused() {
        return Minecraft.getInstance().isWindowActive();
    }

    @Override
    public float baseFov() {
        return Minecraft.getInstance().options.fov().get().floatValue();
    }

    @Override
    public int serverListCount() {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        return list.size();
    }

    @Override
    public String serverListName(int index) {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        return index >= 0 && index < list.size() ? list.get(index).name : "";
    }

    @Override
    public String serverListAddress(int index) {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        return index >= 0 && index < list.size() ? list.get(index).ip : "";
    }

    @Override
    public void connectToServer(int index) {
        Minecraft mc = Minecraft.getInstance();
        ServerList list = new ServerList(mc);
        list.load();
        if (index < 0 || index >= list.size()) {
            return;
        }
        ServerData data = list.get(index);
        Screen parent = mc.gui.screen() != null ? mc.gui.screen() : new JoinMultiplayerScreen(null);
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
                parent, mc, ServerAddress.parseString(data.ip), data, false, null);
    }

    private boolean hudHidden;

    @Override
    public void takeScreenshot() {
        var key = Minecraft.getInstance().options.keyScreenshot;
        key.setDown(true);
        key.setDown(false);
    }

    @Override
    public boolean captureFrame(Path outputPath) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Files.createDirectories(outputPath.getParent());
            final boolean[] ok = {false};
            net.minecraft.client.Screenshot.takeScreenshot(
                    mc.gameRenderer.mainRenderTarget(),
                    image -> {
                        try {
                            image.writeToFile(outputPath);
                            ok[0] = true;
                        } catch (IOException ignored) {
                        } finally {
                            image.close();
                        }
                    });
            return ok[0] && Files.isRegularFile(outputPath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void setHudHidden(boolean hidden) {
        if (hudHidden == hidden) {
            return;
        }
        var key = Minecraft.getInstance().options.keyToggleGui;
        key.setDown(true);
        key.setDown(false);
        hudHidden = hidden;
    }

    @Override
    public boolean isHudHidden() {
        return hudHidden;
    }

    @Override
    public float gamma() {
        return Minecraft.getInstance().options.gamma().get().floatValue();
    }

    @Override
    public void setGamma(float gamma) {
        Minecraft.getInstance().options.gamma().set((double) Math.clamp(gamma, 0.0F, 1.0F));
    }

    private static ItemStack armorStack(int slot) {
        EquipmentSlot[] slots = {
                EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
        };
        if (slot < 0 || slot >= slots.length) {
            return ItemStack.EMPTY;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.getItemBySlot(slots[slot]);
    }

    private static MobEffectInstance effectAt(int index) {
        var effects = Minecraft.getInstance().player.getActiveEffects();
        if (index < 0 || index >= effects.size()) {
            return null;
        }
        int i = 0;
        for (MobEffectInstance effect : effects) {
            if (i++ == index) {
                return effect;
            }
        }
        return null;
    }

    private static ItemContainerContents shulkerContents() {
        ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
        return stack.get(DataComponents.CONTAINER);
    }

    @Override
    public void spawnHitParticles(double x, double y, double z, int color, float size, int count) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int rgb = color & 0xFFFFFF;
        DustParticleOptions options = new DustParticleOptions(rgb, Math.max(0.5f, size));
        var random = mc.level.getRandom();
        for (int i = 0; i < count; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.5;
            double oy = (random.nextDouble() - 0.5) * 0.5;
            double oz = (random.nextDouble() - 0.5) * 0.5;
            mc.particleEngine.createParticle(options, x + ox, y + oy, z + oz, 0, 0.03, 0);
        }
    }

    @Override
    public String translate(String key, String fallback, Object... args) {
        try {
            Component component = args.length == 0
                    ? Component.translatable(key)
                    : Component.translatable(key, args);
            String resolved = component.getString();
            if (resolved.equals(key)) {
                return formatFallback(fallback, args);
            }
            return resolved;
        } catch (Exception ignored) {
            return formatFallback(fallback, args);
        }
    }

    private static String formatFallback(String fallback, Object... args) {
        if (args == null || args.length == 0) {
            return fallback;
        }
        try {
            return String.format(Locale.ROOT, fallback, args);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
