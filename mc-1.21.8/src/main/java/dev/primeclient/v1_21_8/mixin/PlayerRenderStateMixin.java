package dev.primeclient.v1_21_8.mixin;

import dev.primeclient.v1_21_8.render.PrimeCosmeticRenderData;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerRenderState.class)
public abstract class PlayerRenderStateMixin implements PrimeCosmeticRenderData {

    @Unique
    private String primeclient$capeId = "";

    @Unique
    private String primeclient$wingsId = "";

    @Unique
    private String primeclient$hatId = "";

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

    @Override
    public String primeclient$getHatId() {
        return primeclient$hatId;
    }

    @Override
    public void primeclient$setHatId(String hatId) {
        this.primeclient$hatId = hatId != null ? hatId : "";
    }
}
