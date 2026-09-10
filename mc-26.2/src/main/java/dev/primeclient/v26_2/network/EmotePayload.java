package dev.primeclient.v26_2.network;

import dev.primeclient.core.PrimeClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Fabric payload announcing a Prime emote play (C2S announce, S2C relay). */
public record EmotePayload(UUID playerId, String emoteId, long startedAtMs)
        implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(PrimeClient.MOD_ID, "emote");
    public static final Type<EmotePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, EmotePayload> CODEC =
            StreamCodec.of(EmotePayload::encode, EmotePayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, EmotePayload payload) {
        buf.writeLong(payload.playerId().getMostSignificantBits());
        buf.writeLong(payload.playerId().getLeastSignificantBits());
        buf.writeUtf(payload.emoteId() == null ? "" : payload.emoteId());
        buf.writeLong(payload.startedAtMs());
    }

    private static EmotePayload decode(RegistryFriendlyByteBuf buf) {
        return new EmotePayload(
                new UUID(buf.readLong(), buf.readLong()),
                buf.readUtf(),
                buf.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
