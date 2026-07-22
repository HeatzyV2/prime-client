package dev.primeclient.v1_21_11.mixin;

import dev.primeclient.v1_21_11.render.PrimeCosmeticRenderData;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements PrimeCosmeticRenderData {

    @Unique
    private String primeclient$capeId = "";

    @Unique
    private String primeclient$wingsId = "";

    @Override
    public String primeclient$getCapeId() {
        return primeclient$capeId;
    }

    @Override
    public void primeclient$setCapeId(String capeId) {
        this.primeclient$capeId = capeId != null ? capeId : "";
    }

    @Override
    public String primeclient$getWingsId() {
        return primeclient$wingsId;
    }

    @Override
    public void primeclient$setWingsId(String wingsId) {
        this.primeclient$wingsId = wingsId != null ? wingsId : "";
    }
}
