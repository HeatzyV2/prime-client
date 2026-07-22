package dev.primeclient.v1_21_11.mixin;

import dev.primeclient.core.cosmetics.CosmeticLoadout;
import dev.primeclient.core.state.CosmeticsState;
import dev.primeclient.v1_21_11.render.PrimeCosmeticRenderData;
import dev.primeclient.v1_21_11.render.PrimeWingsLayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void primeclient$addWingsLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccessor) this).primeclient$addLayer(new PrimeWingsLayer(
                (AvatarRenderer<?>) (Object) this, context.getModelSet()));
    }

    @Inject(method = "extractCapeState", at = @At("RETURN"))
    private void primeclient$primeCape(Avatar entity, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        boolean local = entity instanceof LocalPlayer;
        CosmeticLoadout loadout = CosmeticsState.loadoutFor(entity.getUUID(), local);
        if (state instanceof PrimeCosmeticRenderData data) {
            data.primeclient$setCapeId(loadout.capeId());
            data.primeclient$setWingsId(loadout.wingsId());
        }
        if (loadout.hasCape()) {
            state.showCape = true;
        }
    }
}
