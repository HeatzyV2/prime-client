package dev.primeclient.v1_21_11.network;

import dev.primeclient.core.PrimeClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Fabric payload announcing a Prime Client player + full cosmetic loadout + skin hash. */
public record PresencePayload(
        UUID playerId,
        String capeId,
        String wingsId,
        String auraId,
        String trailId,
        String hatId,
        String badgeId,
        String skinHash
) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(PrimeClient.MOD_ID, "presence");
    public static final Type<PresencePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PresencePayload> CODEC =
            StreamCodec.of(PresencePayload::encode, PresencePayload::decode);

    public PresencePayload(UUID playerId) {
        this(playerId, "", "", "", "", "", "", "");
    }

    public PresencePayload(UUID playerId, String capeId, String wingsId) {
        this(playerId, capeId, wingsId, "", "", "", "", "");
    }

    public PresencePayload(UUID playerId, String capeId, String wingsId, String skinHash) {
        this(playerId, capeId, wingsId, "", "", "", "", skinHash);
    }

    private static void encode(RegistryFriendlyByteBuf buf, PresencePayload payload) {
        buf.writeLong(payload.playerId().getMostSignificantBits());
        buf.writeLong(payload.playerId().getLeastSignificantBits());
        buf.writeUtf(nullToEmpty(payload.capeId()));
        buf.writeUtf(nullToEmpty(payload.wingsId()));
        buf.writeUtf(nullToEmpty(payload.auraId()));
        buf.writeUtf(nullToEmpty(payload.trailId()));
        buf.writeUtf(nullToEmpty(payload.hatId()));
        buf.writeUtf(nullToEmpty(payload.badgeId()));
        buf.writeUtf(nullToEmpty(payload.skinHash()));
    }

    private static PresencePayload decode(RegistryFriendlyByteBuf buf) {
        UUID id = new UUID(buf.readLong(), buf.readLong());
        return new PresencePayload(
                id,
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
