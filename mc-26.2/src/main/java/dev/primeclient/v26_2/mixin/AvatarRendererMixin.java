package dev.primeclient.v26_2.mixin;

import dev.primeclient.core.cosmetics.CapePhysics;
import dev.primeclient.core.cosmetics.CosmeticLoadout;
import dev.primeclient.core.state.CapePhysicsState;
import dev.primeclient.core.state.CosmeticsState;
import dev.primeclient.v26_2.render.PrimeCosmeticRenderData;
import dev.primeclient.v26_2.render.PrimeHatLayer;
import dev.primeclient.v26_2.render.PrimeWingsLayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
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
        ((LivingEntityRendererAccessor) this).primeclient$addLayer(new PrimeHatLayer(
                (AvatarRenderer<?>) (Object) this, context.getModelSet()));
    }

    @Inject(method = "extractCapeState", at = @At("RETURN"))
    private void primeclient$primeCape(Avatar entity, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        boolean local = entity instanceof LocalPlayer;
        CosmeticLoadout loadout = CosmeticsState.loadoutFor(entity.getUUID(), local);
        if (state instanceof PrimeCosmeticRenderData data) {
            data.primeclient$setCapeId(loadout.capeId());
            data.primeclient$setWingsId(loadout.wingsId());
            data.primeclient$setHatId(loadout.hatId());
        }
        if (loadout.hasCape()) {
            state.showCape = true;
            primeclient$applyCapePhysics(entity, state);
        }
    }

    private static void primeclient$applyCapePhysics(Avatar entity, AvatarRenderState state) {
        if (!CapePhysicsState.active()) {
            return;
        }
        Vec3 velocity = entity.getDeltaMovement();
        float horizontal = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        CapePhysics.Pose pose = CapePhysics.smooth(
                entity.getUUID(),
                state.capeFlap,
                state.capeLean,
                state.capeLean2,
                new CapePhysics.Motion(
                        horizontal,
                        (float) velocity.y,
                        entity.onGround(),
                        entity.isSprinting(),
                        state.ageInTicks),
                CapePhysicsState.intensity());
        state.capeFlap = pose.flap();
        state.capeLean = pose.lean();
        state.capeLean2 = pose.lean2();
    }
}
