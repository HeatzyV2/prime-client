package dev.primeclient.v26_2.render;

/** Accessor for Prime cape/wings IDs stored on {@link net.minecraft.client.renderer.entity.state.AvatarRenderState}. */
public interface PrimeCosmeticRenderData {

    String primeclient$getCapeId();

    void primeclient$setCapeId(String capeId);

    String primeclient$getWingsId();

    void primeclient$setWingsId(String wingsId);
}
