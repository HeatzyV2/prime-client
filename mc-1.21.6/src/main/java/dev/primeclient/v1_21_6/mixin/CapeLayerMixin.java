package dev.primeclient.v1_21_6.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.cosmetics.CosmeticTextures;
import dev.primeclient.v1_21_6.render.PrimeCosmeticRenderData;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {

    @Shadow
    @Final
    private HumanoidModel<PlayerRenderState> model;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void primeclient$renderPrimeCape(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                             PlayerRenderState state, float a, float b, CallbackInfo ci) {
        if (!(state instanceof PrimeCosmeticRenderData data)) {
            return;
        }
        String path = CosmeticTextures.capePath(data.primeclient$getCapeId());
        if (path == null) {
            return;
        }
        if (state.isInvisible || state.chestEquipment.is(Items.ELYTRA)) {
            ci.cancel();
            return;
        }
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(PrimeClient.MOD_ID, path);
        poseStack.pushPose();
        if (!state.chestEquipment.isEmpty()) {
            poseStack.translate(0.0F, -0.053125F, 0.06875F);
        }
        PlayerModel parent = (PlayerModel) ((CapeLayer) (Object) this).getParentModel();
        parent.copyPropertiesTo(this.model);
        this.model.setupAnim(state);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entitySolid(texture));
        this.model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        ci.cancel();
    }
}
