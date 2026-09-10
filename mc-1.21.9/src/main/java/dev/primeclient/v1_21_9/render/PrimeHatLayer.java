package dev.primeclient.v1_21_9.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.cosmetics.CosmeticTextures;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renders Prime cosmetic hats above the player head. */
public final class PrimeHatLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final PrimeHatModel model;

    public PrimeHatLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
        super(parent);
        this.model = new PrimeHatModel(PrimeHatModel.createBodyLayer().bakeRoot());
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float limbSwing, float limbSwingAmount) {
        if (!(state instanceof PrimeCosmeticRenderData data)) {
            return;
        }
        if (state.isInvisible) {
            return;
        }
        String path = CosmeticTextures.hatPath(data.primeclient$getHatId());
        if (path == null) {
            return;
        }
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(PrimeClient.MOD_ID, path);
        model.setupAnim(state);
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.05F, 0.0F);
        collector.submitModel(
                model,
                state,
                poseStack,
                RenderType.entityTranslucent(texture),
                light,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null);
        poseStack.popPose();
    }
}
