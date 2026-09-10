package dev.primeclient.v1_21_7.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.primeclient.v1_21_7.render.CustomSkinTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {

    @Shadow
    public abstract GameProfile getProfile();

    @ModifyReturnValue(method = "getSkin", at = @At("RETURN"))
    private PlayerSkin primeclient$customSkin(PlayerSkin original) {
        GameProfile profile = getProfile();
        if (profile == null) {
            return original;
        }
        UUID uuid = profile.getId();
        Minecraft client = Minecraft.getInstance();
        boolean local = client.player != null && uuid.equals(client.player.getUUID());
        return CustomSkinTextures.maybeOverride(uuid, local, original);
    }
}
