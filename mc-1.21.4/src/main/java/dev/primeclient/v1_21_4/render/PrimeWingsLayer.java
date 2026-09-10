package dev.primeclient.v1_21_4.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.cosmetics.CosmeticTextures;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Renders Prime cosmetic wings behind the player. */
public final class PrimeWingsLayer extends RenderLayer<PlayerRenderState, PlayerModel> {

    private final PrimeWingsModel model;

    public PrimeWingsLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent, EntityModelSet models) {
        super(parent);
        this.model = new PrimeWingsModel(PrimeWingsModel.createBodyLayer().bakeRoot());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                       PlayerRenderState state, float limbSwing, float limbSwingAmount) {
        if (!(state instanceof PrimeCosmeticRenderData data)) {
            return;
        }
        if (state.isInvisible) {
            return;
        }
        String path = CosmeticTextures.wingsPath(data.primeclient$getWingsId());
        if (path == null) {
            return;
        }
        if (state.chestEquipment.is(Items.ELYTRA)) {
            return;
        }
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(PrimeClient.MOD_ID, path);
        model.setupAnim(state);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.12F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
