package dev.primeclient.v26_2.network;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.hook.PrimeHooks;
import dev.primeclient.core.state.ClientBadgeState;
import dev.primeclient.core.state.CosmeticsState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Registers Prime presence payloads for LAN and integrated-server play (MC 26.2). */
public final class PresenceNetworking {

    private PresenceNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(PresencePayload.TYPE, PresencePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PresencePayload.TYPE, PresencePayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(PresencePayload.TYPE, (payload, context) ->
                context.client().execute(() -> PrimeHooks.onPresencePayload(
                        payload.playerId(), payload.capeId(), payload.wingsId())));

        PrimeClient.get().presence().setNetworkAnnouncer(PresenceNetworking::sendLocalPresence);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientBadgeState.active()) {
                client.execute(PresenceNetworking::sendLocalPresence);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                ServerPlayNetworking.registerGlobalReceiver(PresencePayload.TYPE, (payload, context) -> {
                    ServerPlayer senderPlayer = context.player();
                    UUID senderId = senderPlayer.getUUID();
                    PresencePayload forward = new PresencePayload(senderId, payload.capeId(), payload.wingsId());
                    for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                        if (target == senderPlayer) {
                            continue;
                        }
                        ServerPlayNetworking.send(target, forward);
                    }
                }));
    }

    private static void sendLocalPresence() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ClientPlayNetworking.canSend(PresencePayload.TYPE)) {
            return;
        }
        ClientPlayNetworking.send(new PresencePayload(
                client.player.getUUID(),
                CosmeticsState.localCapeId(),
                CosmeticsState.localWingsId()));
    }
}
