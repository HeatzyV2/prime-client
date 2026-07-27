package dev.primeclient.v26_2.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.state.CustomSkinState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Registers DynamicTextures for Prime custom skins and builds overridden PlayerSkin values. */
public final class CustomSkinTextures {

    private record Entry(Identifier id, String hash, DynamicTexture texture) {
    }

    private static final Map<UUID, Entry> BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<String, Entry> BY_HASH = new ConcurrentHashMap<>();

    private CustomSkinTextures() {
    }

    public static PlayerSkin maybeOverride(UUID uuid, boolean localPlayer, PlayerSkin original) {
        if (uuid == null || original == null) {
            return original;
        }
        byte[] png = CustomSkinState.bytesFor(uuid, localPlayer);
        if (png == null || png.length == 0) {
            return original;
        }
        String hash = localPlayer ? CustomSkinState.localHash() : CustomSkinState.peerHash(uuid);
        if (hash == null || hash.isBlank()) {
            hash = CustomSkinState.hashOf(png);
        }
        Identifier bodyId = ensureRegistered(uuid, hash, png);
        if (bodyId == null) {
            return original;
        }
        ClientAsset.Texture body = new ClientAsset.ResourceTexture(bodyId);
        return PlayerSkin.insecure(body, original.cape(), original.elytra(), original.model());
    }

    private static Identifier ensureRegistered(UUID uuid, String hash, byte[] png) {
        Entry byHash = BY_HASH.get(hash);
        if (byHash != null) {
            BY_PLAYER.put(uuid, byHash);
            return byHash.id();
        }
        Entry existing = BY_PLAYER.get(uuid);
        if (existing != null && hash.equals(existing.hash())) {
            return existing.id();
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
            Identifier id = Identifier.fromNamespaceAndPath(
                    PrimeClient.MOD_ID, "skins/" + uuid.toString().replace("-", ""));
            DynamicTexture texture = new DynamicTexture(() -> "prime-skin-" + hash.substring(0, Math.min(8, hash.length())), image);
            client.getTextureManager().register(id, texture);
            Entry entry = new Entry(id, hash, texture);
            BY_PLAYER.put(uuid, entry);
            BY_HASH.put(hash, entry);
            return id;
        } catch (IOException e) {
            PrimeClient.LOGGER.warn("Failed to upload custom skin for {}: {}", uuid, e.toString());
            return null;
        }
    }
}
