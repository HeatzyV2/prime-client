package dev.primeclient.v1_21_11.network;

import dev.primeclient.core.PrimeClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Fabric payload announcing a Prime Client player on LAN / integrated servers. */
public record PresencePayload(UUID playerId) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(PrimeClient.MOD_ID, "presence");
    public static final Type<PresencePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PresencePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    payload -> payload.playerId().getMostSignificantBits(),
                    ByteBufCodecs.VAR_LONG,
                    payload -> payload.playerId().getLeastSignificantBits(),
                    (msb, lsb) -> new PresencePayload(new UUID(msb, lsb)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
