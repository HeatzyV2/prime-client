package dev.primeclient.core.adapter;

import java.nio.file.Path;

/**
 * Version-independent gateway to Minecraft.
 *
 * <p>This is the only door between the common core and the game. Each
 * supported Minecraft version ships its own implementation in its version
 * layer. Core code must never reference Minecraft classes directly.</p>
 *
 * <p>New methods use {@code default} implementations so test doubles stay
 * minimal. Version layers override what they support.</p>
 */
public interface MinecraftAdapter {

    String minecraftVersion();

    Path gameDirectory();

    Path configDirectory();

    boolean isInGame();

    boolean isScreenOpen();

    boolean isKeyDown(int glfwKey);

    boolean isMouseButtonDown(int glfwButton);

    int fps();

    boolean hasPlayer();

    double playerX();

    double playerY();

    double playerZ();

    void runOnClientThread(Runnable task);

    void openHudEditor();

    void openClickGui();

    /** Opens the vanilla world selection screen from the title menu. */
    default void openSingleplayer() {
    }

    /** Opens the vanilla multiplayer browser from the title menu. */
    default void openMultiplayer() {
    }

    /** Opens vanilla game options from the title menu. */
    default void openOptions() {
    }

    /** Closes the game from the title menu. */
    default void quitGame() {
    }

    /** Closes the active screen and returns to the game. */
    default void closeCurrentScreen() {
    }

    /** Multiplayer latency in ms, or {@code 0} when unavailable. */
    default int ping() {
        return 0;
    }

    /** Current server address, or {@code "Singleplayer"}. */
    default String serverAddress() {
        return "";
    }

    // --- player ---

    default float playerYaw() {
        return 0;
    }

    default float playerPitch() {
        return 0;
    }

    default String playerName() {
        return "";
    }

    default float playerHealth() {
        return 0;
    }

    default float playerMaxHealth() {
        return 20;
    }

    default boolean isSprinting() {
        return false;
    }

    default void setSprinting(boolean sprint) {
    }

    default boolean isSneaking() {
        return false;
    }

    default void setSneaking(boolean sneak) {
    }

    /** Whether the forward movement key is held. */
    default boolean isMovingForward() {
        return false;
    }

    default boolean isDead() {
        return false;
    }

    default void respawn() {
    }

    // --- combat / target ---

    default boolean hasTarget() {
        return false;
    }

    default String targetName() {
        return "";
    }

    default float targetHealth() {
        return 0;
    }

    default float targetMaxHealth() {
        return 20;
    }

    default float attackCooldown() {
        return 1.0f;
    }

    // --- armor (0=boots, 1=leggings, 2=chest, 3=helmet) ---

    default int armorSlotCount() {
        return 4;
    }

    default boolean hasArmor(int slot) {
        return false;
    }

    default int armorDurability(int slot) {
        return 0;
    }

    default int armorMaxDurability(int slot) {
        return 0;
    }

    // --- potion effects ---

    default int potionEffectCount() {
        return 0;
    }

    default String potionName(int index) {
        return "";
    }

    default int potionDurationSeconds(int index) {
        return 0;
    }

    default int potionAmplifier(int index) {
        return 0;
    }

    // --- items ---

    default String heldItemName() {
        return "";
    }

    default int heldItemCount() {
        return 0;
    }

    default String hoveredItemName() {
        return "";
    }

    default boolean hoveredItemIsShulkerBox() {
        return false;
    }

    default int shulkerSlotCount() {
        return 0;
    }

    default String shulkerSlotItem(int index) {
        return "";
    }

    default int shulkerSlotCount(int index) {
        return 0;
    }

    // --- memory ---

    default long usedMemoryMb() {
        return 0;
    }

    default long maxMemoryMb() {
        return 0;
    }

    default void suggestGarbageCollection() {
    }

    // --- chat ---

    default void sendChat(String message) {
    }

    // --- session ---

    /** Milliseconds since the player joined the current world. */
    default long sessionMillis() {
        return 0;
    }

    // --- world ---

    default String facingDirection() {
        return "N";
    }

    default String biomeName() {
        return "";
    }

    // --- options / performance ---

    default int renderDistance() {
        return 12;
    }

    default void setRenderDistance(int chunks) {
    }

    default int simulationDistance() {
        return 12;
    }

    default void setSimulationDistance(int chunks) {
    }

    default int maxFps() {
        return 120;
    }

    default void setMaxFps(int fps) {
    }

    /** 0 = all, 1 = decreased, 2 = minimal. */
    default int particleSetting() {
        return 0;
    }

    default void setParticleSetting(int setting) {
    }

    default boolean cloudsEnabled() {
        return true;
    }

    default void setCloudsEnabled(boolean enabled) {
    }

    default int entityDistance() {
        return 100;
    }

    default void setEntityDistance(int percent) {
    }

    default boolean fancyGraphics() {
        return true;
    }

    default void setFancyGraphics(boolean enabled) {
    }

    default boolean isWindowFocused() {
        return true;
    }

    // --- camera / FOV ---

    default float baseFov() {
        return 70;
    }

    // --- servers ---

    default int serverListCount() {
        return 0;
    }

    default String serverListName(int index) {
        return "";
    }

    default String serverListAddress(int index) {
        return "";
    }

    default void connectToServer(int index) {
    }

    // --- screenshot / HUD ---

    default void takeScreenshot() {
    }

    /** Writes the current framebuffer to {@code outputPath} (PNG). Used by clip recorder. */
    default boolean captureFrame(java.nio.file.Path outputPath) {
        return false;
    }

    default void setHudHidden(boolean hidden) {
    }

    default boolean isHudHidden() {
        return false;
    }

    /** Spawns client-side hit particles at world coordinates. */
    default void spawnHitParticles(double x, double y, double z, int color, float size, int count) {
    }
}
