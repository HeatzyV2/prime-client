package dev.primeclient.v26_2.network;

import dev.primeclient.core.PrimeClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Fabric payload announcing a Prime Client player + cosmetic loadout on LAN / integrated servers. */
public record PresencePayload(UUID playerId, String capeId, String wingsId) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(PrimeClient.MOD_ID, "presence");
    public static final Type<PresencePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PresencePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    payload -> payload.playerId().getMostSignificantBits(),
                    ByteBufCodecs.VAR_LONG,
                    payload -> payload.playerId().getLeastSignificantBits(),
                    ByteBufCodecs.STRING_UTF8,
                    PresencePayload::capeId,
                    ByteBufCodecs.STRING_UTF8,
                    PresencePayload::wingsId,
                    (msb, lsb, capeId, wingsId) -> new PresencePayload(new UUID(msb, lsb), capeId, wingsId));

    public PresencePayload(UUID playerId) {
        this(playerId, "", "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
