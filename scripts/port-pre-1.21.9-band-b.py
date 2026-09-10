#!/usr/bin/env python3
"""Port scaffolded 1.21.11-style layers down to ≤1.21.8 (Band B).

Changes:
- Screen input: KeyEvent/MouseButtonEvent/CharacterEvent → primitive overloads
- Remove panoramaShouldSpin overrides (1.21.9+)
- AvatarRenderer/AvatarRenderState/Avatar → PlayerRenderer/PlayerRenderState/AbstractClientPlayer
- submit/SubmitNodeCollector → render/MultiBufferSource
- Custom skins: ClientAsset + world.entity.player.PlayerSkin → client.resources.PlayerSkin
- Rename mixin classes + mixins.json entries
"""

from __future__ import annotations

import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSIONS = ["1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8"]


def pkg(version: str) -> str:
    return "v" + version.replace(".", "_")


def rewrite_screen(text: str) -> str:
    text = re.sub(r"import net\.minecraft\.client\.input\.CharacterEvent;\n", "", text)
    text = re.sub(r"import net\.minecraft\.client\.input\.KeyEvent;\n", "", text)
    text = re.sub(r"import net\.minecraft\.client\.input\.MouseButtonEvent;\n", "", text)

    # Drop panoramaShouldSpin overrides entirely (method absent ≤1.21.8).
    text = re.sub(
        r"\n\s*@Override\n\s*public boolean panoramaShouldSpin\(\) \{\n\s*return true;\n\s*\}\n",
        "\n",
        text,
    )

    # mouseClicked
    text = re.sub(
        r"public boolean mouseClicked\(MouseButtonEvent event, boolean doubleClick\) \{",
        "public boolean mouseClicked(double mouseX, double mouseY, int button) {",
        text,
    )
    text = text.replace("event.x()", "mouseX")
    text = text.replace("event.y()", "mouseY")
    text = text.replace("event.button()", "button")
    text = text.replace("super.mouseClicked(event, doubleClick)", "super.mouseClicked(mouseX, mouseY, button)")

    # mouseDragged
    text = re.sub(
        r"public boolean mouseDragged\(MouseButtonEvent event, double dragX, double dragY\) \{",
        "public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {",
        text,
    )
    text = text.replace("super.mouseDragged(event, dragX, dragY)", "super.mouseDragged(mouseX, mouseY, button, dragX, dragY)")

    # mouseReleased
    text = re.sub(
        r"public boolean mouseReleased\(MouseButtonEvent event\) \{",
        "public boolean mouseReleased(double mouseX, double mouseY, int button) {",
        text,
    )
    text = text.replace("super.mouseReleased(event)", "super.mouseReleased(mouseX, mouseY, button)")

    # charTyped
    text = re.sub(
        r"public boolean charTyped\((?:net\.minecraft\.client\.input\.)?CharacterEvent event\) \{",
        "public boolean charTyped(char codePoint, int modifiers) {",
        text,
    )
    text = text.replace("(char) event.codepoint()", "codePoint")
    text = text.replace("super.charTyped(event)", "super.charTyped(codePoint, modifiers)")

    # keyPressed
    text = re.sub(
        r"public boolean keyPressed\(KeyEvent event\) \{",
        "public boolean keyPressed(int keyCode, int scanCode, int modifiers) {",
        text,
    )
    text = text.replace("event.key()", "keyCode")
    # Hud editor used event.hasShiftDown()/hasControlDown()
    text = text.replace("event.hasShiftDown()", "hasShiftDown()")
    text = text.replace("event.hasControlDown()", "hasControlDown()")
    text = text.replace("super.keyPressed(event)", "super.keyPressed(keyCode, scanCode, modifiers)")

    # Minecraft instance modifier helpers → Screen statics (exist on ≤1.21.8)
    text = text.replace("Minecraft.getInstance().hasAltDown()", "hasAltDown()")
    text = text.replace("Minecraft.getInstance().hasShiftDown()", "hasShiftDown()")
    text = text.replace("Minecraft.getInstance().hasControlDown()", "hasControlDown()")

    return text


def rewrite_avatar_symbols(text: str) -> str:
    text = text.replace(
        "import net.minecraft.client.renderer.entity.player.AvatarRenderer;",
        "import net.minecraft.client.renderer.entity.player.PlayerRenderer;",
    )
    text = text.replace(
        "import net.minecraft.client.renderer.entity.state.AvatarRenderState;",
        "import net.minecraft.client.renderer.entity.state.PlayerRenderState;",
    )
    text = text.replace(
        "import net.minecraft.world.entity.Avatar;",
        "import net.minecraft.client.player.AbstractClientPlayer;",
    )
    text = text.replace(
        "import net.minecraft.client.renderer.SubmitNodeCollector;",
        "import net.minecraft.client.renderer.MultiBufferSource;",
    )

    text = re.sub(r"\bAvatarRenderer\b", "PlayerRenderer", text)
    text = re.sub(r"\bAvatarRenderState\b", "PlayerRenderState", text)
    # Entity type Avatar → AbstractClientPlayer (careful not to touch AbstractClientPlayer already)
    text = re.sub(r"\bAvatar\b", "AbstractClientPlayer", text)

    # Method rename submit → render for layer overrides / injects
    text = text.replace('method = "submit"', 'method = "render"')
    text = re.sub(
        r"public void submit\(PoseStack poseStack, SubmitNodeCollector collector, int light,",
        "public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light,",
        text,
    )
    text = re.sub(
        r"private void primeclient\$renderPrimeCape\(PoseStack poseStack, SubmitNodeCollector collector, int light,",
        "private void primeclient$renderPrimeCape(PoseStack poseStack, MultiBufferSource bufferSource, int light,",
        text,
    )

    # collector.submitModel(...) → buffer getBuffer + renderToBuffer
    # Handled per-file below if still present.
    return text


CAPE_LAYER = '''package {package}.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.cosmetics.CosmeticTextures;
import {package}.render.PrimeCosmeticRenderData;
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
public abstract class CapeLayerMixin {{

    @Shadow
    @Final
    private HumanoidModel<PlayerRenderState> model;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void primeclient$renderPrimeCape(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                             PlayerRenderState state, float a, float b, CallbackInfo ci) {{
        if (!(state instanceof PrimeCosmeticRenderData data)) {{
            return;
        }}
        String path = CosmeticTextures.capePath(data.primeclient$getCapeId());
        if (path == null) {{
            return;
        }}
        if (state.isInvisible || state.chestEquipment.is(Items.ELYTRA)) {{
            ci.cancel();
            return;
        }}
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(PrimeClient.MOD_ID, path);
        poseStack.pushPose();
        if (!state.chestEquipment.isEmpty()) {{
            poseStack.translate(0.0F, -0.053125F, 0.06875F);
        }}
        PlayerModel parent = (PlayerModel) ((CapeLayer) (Object) this).getParentModel();
        parent.copyPropertiesTo(this.model);
        this.model.setupAnim(state);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entitySolid(texture));
        this.model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        ci.cancel();
    }}
}}
'''

PLAYER_RENDERER_MIXIN = '''package {package}.mixin;

import dev.primeclient.core.cosmetics.CapePhysics;
import dev.primeclient.core.cosmetics.CosmeticLoadout;
import dev.primeclient.core.state.CapePhysicsState;
import dev.primeclient.core.state.CosmeticsState;
import {package}.render.PrimeCosmeticRenderData;
import {package}.render.PrimeHatLayer;
import {package}.render.PrimeWingsLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {{

    @Inject(method = "<init>", at = @At("RETURN"))
    private void primeclient$addWingsLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {{
        ((LivingEntityRendererAccessor) this).primeclient$addLayer(new PrimeWingsLayer(
                (PlayerRenderer) (Object) this, context.getModelSet()));
        ((LivingEntityRendererAccessor) this).primeclient$addLayer(new PrimeHatLayer(
                (PlayerRenderer) (Object) this, context.getModelSet()));
    }}

    @Inject(method = "extractCapeState", at = @At("RETURN"))
    private static void primeclient$primeCape(AbstractClientPlayer entity, PlayerRenderState state,
                                              float tickDelta, CallbackInfo ci) {{
        boolean local = entity instanceof LocalPlayer;
        CosmeticLoadout loadout = CosmeticsState.loadoutFor(entity.getUUID(), local);
        if (state instanceof PrimeCosmeticRenderData data) {{
            data.primeclient$setCapeId(loadout.capeId());
            data.primeclient$setWingsId(loadout.wingsId());
            data.primeclient$setHatId(loadout.hatId());
        }}
        if (loadout.hasCape()) {{
            state.showCape = true;
            primeclient$applyCapePhysics(entity, state);
        }}
    }}

    private static void primeclient$applyCapePhysics(AbstractClientPlayer entity, PlayerRenderState state) {{
        if (!CapePhysicsState.active()) {{
            return;
        }}
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
    }}
}}
'''

PLAYER_RENDER_STATE_MIXIN = '''package {package}.mixin;

import {package}.render.PrimeCosmeticRenderData;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerRenderState.class)
public abstract class PlayerRenderStateMixin implements PrimeCosmeticRenderData {{

    @Unique
    private String primeclient$capeId = "";

    @Unique
    private String primeclient$wingsId = "";

    @Unique
    private String primeclient$hatId = "";

    @Override
    public String primeclient$getCapeId() {{
        return primeclient$capeId;
    }}

    @Override
    public void primeclient$setCapeId(String capeId) {{
        this.primeclient$capeId = capeId != null ? capeId : "";
    }}

    @Override
    public String primeclient$getWingsId() {{
        return primeclient$wingsId;
    }}

    @Override
    public void primeclient$setWingsId(String wingsId) {{
        this.primeclient$wingsId = wingsId != null ? wingsId : "";
    }}

    @Override
    public String primeclient$getHatId() {{
        return primeclient$hatId;
    }}

    @Override
    public void primeclient$setHatId(String hatId) {{
        this.primeclient$hatId = hatId != null ? hatId : "";
    }}
}}
'''

HAT_LAYER = '''package {package}.render;

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

/** Renders Prime cosmetic hats above the player head. */
public final class PrimeHatLayer extends RenderLayer<PlayerRenderState, PlayerModel> {{

    private final PrimeHatModel model;

    public PrimeHatLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent, EntityModelSet models) {{
        super(parent);
        this.model = new PrimeHatModel(PrimeHatModel.createBodyLayer().bakeRoot());
    }}

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                       PlayerRenderState state, float limbSwing, float limbSwingAmount) {{
        if (!(state instanceof PrimeCosmeticRenderData data)) {{
            return;
        }}
        if (state.isInvisible) {{
            return;
        }}
        String path = CosmeticTextures.hatPath(data.primeclient$getHatId());
        if (path == null) {{
            return;
        }}
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(PrimeClient.MOD_ID, path);
        model.setupAnim(state);
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.05F, 0.0F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }}
}}
'''

WINGS_LAYER = '''package {package}.render;

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

/** Renders animated Prime cosmetic wings behind the player. */
public final class PrimeWingsLayer extends RenderLayer<PlayerRenderState, PlayerModel> {{

    private final PrimeWingsModel model;

    public PrimeWingsLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent, EntityModelSet models) {{
        super(parent);
        this.model = new PrimeWingsModel(PrimeWingsModel.createBodyLayer().bakeRoot());
    }}

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                       PlayerRenderState state, float limbSwing, float limbSwingAmount) {{
        if (!(state instanceof PrimeCosmeticRenderData data)) {{
            return;
        }}
        if (state.isInvisible) {{
            return;
        }}
        String path = CosmeticTextures.wingsPath(data.primeclient$getWingsId());
        if (path == null) {{
            return;
        }}
        if (state.chestEquipment.is(Items.ELYTRA)) {{
            return;
        }}
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(PrimeClient.MOD_ID, path);
        model.setupAnim(state);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.12F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }}
}}
'''


def rewrite_custom_skins(text: str) -> str:
    text = text.replace(
        "import net.minecraft.core.ClientAsset;\n",
        "",
    )
    text = text.replace(
        "import net.minecraft.world.entity.player.PlayerSkin;",
        "import net.minecraft.client.resources.PlayerSkin;",
    )
    # Replace ClientAsset usage block
    text = re.sub(
        r"\s*// id == texturePath so TextureManager lookup hits the DynamicTexture we registered\.\n"
        r"\s*ClientAsset\.Texture body = new ClientAsset\.ResourceTexture\(bodyId, bodyId\);\n"
        r"\s*return PlayerSkin\.insecure\(body, original\.cape\(\), original\.elytra\(\), original\.model\(\)\);",
        """
        return new PlayerSkin(
                bodyId,
                original.textureUrl(),
                original.capeTexture(),
                original.elytraTexture(),
                original.model(),
                false);""",
        text,
    )
    # Doc comment cleanup
    text = re.sub(
        r"<p>Important:.*?TextureManager\.</p>\n",
        "",
        text,
        flags=re.S,
    )
    return text


def rewrite_player_info(text: str) -> str:
    return text.replace(
        "import net.minecraft.world.entity.player.PlayerSkin;",
        "import net.minecraft.client.resources.PlayerSkin;",
    )


def port_version(version: str) -> None:
    module = ROOT / f"mc-{version}"
    java_root = module / "src" / "main" / "java" / "dev" / "primeclient" / pkg(version)
    resources = module / "src" / "main" / "resources"
    package = f"dev.primeclient.{pkg(version)}"

    # Screens
    for path in (java_root / "screen").glob("*.java"):
        path.write_text(rewrite_screen(path.read_text(encoding="utf-8")), encoding="utf-8")
        print(f"screen {path.relative_to(ROOT)}")

    # Models: AvatarRenderState → PlayerRenderState
    for name in ("PrimeHatModel.java", "PrimeWingsModel.java"):
        path = java_root / "render" / name
        if path.exists():
            text = rewrite_avatar_symbols(path.read_text(encoding="utf-8"))
            path.write_text(text, encoding="utf-8")
            print(f"model {path.relative_to(ROOT)}")

    # Layers rewritten wholesale
    (java_root / "render" / "PrimeHatLayer.java").write_text(
        HAT_LAYER.format(package=package), encoding="utf-8"
    )
    (java_root / "render" / "PrimeWingsLayer.java").write_text(
        WINGS_LAYER.format(package=package), encoding="utf-8"
    )
    print(f"layers {version}")

    # Skins
    skins = java_root / "render" / "CustomSkinTextures.java"
    if skins.exists():
        skins.write_text(rewrite_custom_skins(skins.read_text(encoding="utf-8")), encoding="utf-8")
        print(f"skins {skins.relative_to(ROOT)}")

    player_info = java_root / "mixin" / "PlayerInfoMixin.java"
    if player_info.exists():
        player_info.write_text(rewrite_player_info(player_info.read_text(encoding="utf-8")), encoding="utf-8")

    # Mixins: rewrite cape, replace avatar mixins with player mixins
    mixin_dir = java_root / "mixin"
    (mixin_dir / "CapeLayerMixin.java").write_text(
        CAPE_LAYER.format(package=package), encoding="utf-8"
    )
    (mixin_dir / "PlayerRendererMixin.java").write_text(
        PLAYER_RENDERER_MIXIN.format(package=package), encoding="utf-8"
    )
    (mixin_dir / "PlayerRenderStateMixin.java").write_text(
        PLAYER_RENDER_STATE_MIXIN.format(package=package), encoding="utf-8"
    )
    for obsolete in ("AvatarRendererMixin.java", "AvatarRenderStateMixin.java"):
        old = mixin_dir / obsolete
        if old.exists():
            old.unlink()
            print(f"removed {old.relative_to(ROOT)}")

    mixins_json = resources / "primeclient.mixins.json"
    data = json.loads(mixins_json.read_text(encoding="utf-8"))
    client = data.get("client", [])
    client = [
        (
            "PlayerRendererMixin"
            if item == "AvatarRendererMixin"
            else "PlayerRenderStateMixin"
            if item == "AvatarRenderStateMixin"
            else item
        )
        for item in client
    ]
    data["client"] = client
    mixins_json.write_text(json.dumps(data, indent="\t") + "\n", encoding="utf-8")
    print(f"mixins.json {version}")


def main() -> None:
    for version in VERSIONS:
        if not (ROOT / f"mc-{version}").exists():
            print(f"skip missing mc-{version}")
            continue
        print(f"=== porting mc-{version} ===")
        port_version(version)
    print("done")


if __name__ == "__main__":
    main()
