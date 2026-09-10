package dev.primeclient.v26_2.network;

import dev.primeclient.core.PrimeClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** JSON body on the {@code primeclient:main} plugin channel. */
public record MainPayload(String json) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(PrimeClient.MOD_ID, "main");
    public static final Type<MainPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, MainPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    MainPayload::json,
                    MainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
