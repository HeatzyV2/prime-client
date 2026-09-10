package dev.primeclient.v1_21_6;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.hook.PrimeHooks;
import dev.primeclient.core.gui.menu.EmoteWheelRenderer;
import dev.primeclient.core.gui.menu.RadialMenuRenderer;
import dev.primeclient.core.state.EmoteState;
import dev.primeclient.core.state.RadialMenuState;
import dev.primeclient.v1_21_6.render.GuiRenderContext;
import dev.primeclient.v1_21_6.network.MainNetworking;
import dev.primeclient.v1_21_6.network.PresenceNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

/**
 * Fabric client entrypoint for the Minecraft 1.21.6 layer.
 */
public final class PrimeClientEntrypoint implements ClientModInitializer {

    private VersionAdapter adapter;

    @Override
    public void onInitializeClient() {
        adapter = new VersionAdapter();
        PrimeClient.bootstrap(adapter);
        PresenceNetworking.register();
        MainNetworking.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                client.getTextureManager().getTexture(
                        ResourceLocation.fromNamespaceAndPath(PrimeClient.MOD_ID, "textures/gui/logo.png")));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            trackHealth();
            PrimeClient.get().tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            adapter.markSessionStart();
            PrimeClient.get().onWorldJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            adapter.markSessionEnd();
            PrimeClient.get().onWorldLeave();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PrimeClient.get().shutdown());

        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                PrimeHooks.onChatMessage(message.getString(), false));
        ClientSendMessageEvents.ALLOW_CHAT.register(PrimeHooks::allowOutgoingChat);
        ClientSendMessageEvents.ALLOW_COMMAND.register(PrimeHooks::allowOutgoingCommand);
        ClientSendMessageEvents.CHAT.register(message ->
                PrimeHooks.onChatMessage(message, true));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player != null && entity != null) {
                PrimeHooks.onAttackEntity(entity.getName().getString());
            }
            return InteractionResult.PASS;
        });

        GuiRenderContext renderContext = new GuiRenderContext();
        EmoteWheelRenderer emoteWheel = new EmoteWheelRenderer();
        final boolean[] wasLeftDown = {false};
        HudElementRegistry.addLast(
                ResourceLocation.fromNamespaceAndPath(PrimeClient.MOD_ID, "hud"),
                (graphics, deltaTracker) -> {
                    if (dev.primeclient.core.hud.editor.HudEditorState.isActive()) {
                        return;
                    }
                    renderContext.prepare(graphics);
                    var client = PrimeClient.get();
                    client.hud().render(renderContext);
                    if (client.loadingOverlay().visible()) {
                        client.loadingOverlay().render(renderContext, client.themes().active());
                    }
                    if (EmoteState.wheelOpen() || RadialMenuState.open()) {
                        var mc = net.minecraft.client.Minecraft.getInstance();
                        double mx = mc.mouseHandler.xpos()
                                * mc.getWindow().getGuiScaledWidth() / Math.max(1, mc.getWindow().getScreenWidth());
                        double my = mc.mouseHandler.ypos()
                                * mc.getWindow().getGuiScaledHeight() / Math.max(1, mc.getWindow().getScreenHeight());
                        if (EmoteState.wheelOpen()) {
                            emoteWheel.render(renderContext, client.themes().active(),
                                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mx, my);
                        }
                        if (RadialMenuState.open()) {
                            RadialMenuRenderer.render(renderContext, client.themes().active(), mx, my);
                        }
                    }
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean left = client.mouseHandler.isLeftPressed();
            if (left && !wasLeftDown[0]) {
                double mx = client.mouseHandler.xpos()
                        * client.getWindow().getGuiScaledWidth() / Math.max(1, client.getWindow().getScreenWidth());
                double my = client.mouseHandler.ypos()
                        * client.getWindow().getGuiScaledHeight() / Math.max(1, client.getWindow().getScreenHeight());
                int sw = client.getWindow().getGuiScaledWidth();
                int sh = client.getWindow().getGuiScaledHeight();
                if (EmoteState.wheelOpen()) {
                    emoteWheel.mousePressed(mx, my, sw, sh);
                } else if (RadialMenuState.open()) {
                    var pc = PrimeClient.get();
                    RadialMenuRenderer.mousePressed(mx, my, sw, sh,
                            pc.profiles(), pc.adapter(), pc.modules());
                }
            }
            wasLeftDown[0] = left;
        });
    }

    private float lastHealth = -1;

    private void trackHealth() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            lastHealth = -1;
            return;
        }
        float health = player.getHealth();
        if (lastHealth >= 0 && health < lastHealth) {
            PrimeHooks.onPlayerDamage(lastHealth - health);
        }
        if (player.isDeadOrDying() && lastHealth > 0) {
            PrimeHooks.onPlayerDeath(player.getX(), player.getY(), player.getZ());
        }
        lastHealth = health;
    }
}
