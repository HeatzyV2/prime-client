package dev.primeclient.v26_2.render;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** {@link RenderContext} over 26.2's extract-based GUI pipeline. */
public final class GuiRenderContext implements RenderContext {

    private GuiGraphicsExtractor extractor;
    private Font font;
    private float drawOpacity = 1f;

    public void prepare(GuiGraphicsExtractor extractor) {
        this.extractor = extractor;
        this.font = Minecraft.getInstance().font;
        this.drawOpacity = 1f;
    }

    @Override
    public int screenWidth() {
        return extractor.guiWidth();
    }

    @Override
    public int screenHeight() {
        return extractor.guiHeight();
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        extractor.fill(x, y, x + width, y + height, applyOpacity(argb));
    }

    @Override
    public void drawText(String text, int x, int y, int argb, boolean shadow) {
        extractor.text(font, text, x, y, applyOpacity(argb), shadow);
    }

    @Override
    public void drawUiText(String text, int x, int y, int argb) {
        extractor.text(font, text, x, y, applyOpacity(argb), false);
    }

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int uiTextWidth(String text) {
        return font.width(text);
    }

    @Override
    public int fontHeight() {
        return font.lineHeight;
    }

    @Override
    public int uiFontHeight() {
        return font.lineHeight;
    }

    @Override
    public void fillGradientVertical(int x, int y, int width, int height, int topArgb, int bottomArgb) {
        extractor.fillGradient(x, y, x + width, y + height,
                applyOpacity(topArgb), applyOpacity(bottomArgb));
    }

    @Override
    public void pushTransform(float translateX, float translateY, float scale,
                              float rotationDegrees, float pivotLocalX, float pivotLocalY) {
        var pose = extractor.pose();
        pose.pushMatrix();
        pose.translate(translateX, translateY);
        if (rotationDegrees != 0f) {
            pose.rotate((float) Math.toRadians(rotationDegrees));
        }
        pose.scale(scale, scale);
        if (pivotLocalX != 0f || pivotLocalY != 0f) {
            pose.translate(-pivotLocalX, -pivotLocalY);
        }
    }

    @Override
    public void popTransform() {
        extractor.pose().popMatrix();
    }

    @Override
    public void setDrawOpacity(float opacity) {
        this.drawOpacity = Math.clamp(opacity, 0f, 1f);
    }

    @Override
    public void pushClip(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int sw = screenWidth();
        int sh = screenHeight();
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(sw, x + width);
        int y1 = Math.min(sh, y + height);
        if (x1 > x0 && y1 > y0) {
            extractor.enableScissor(x0, y0, x1, y1);
        }
    }

    @Override
    public void popClip() {
        extractor.disableScissor();
    }

    @Override
    public void drawTexture(String texturePath, int x, int y, int width, int height,
                              int textureWidth, int textureHeight, int tintArgb) {
        Identifier id = textureId(texturePath);
        extractor.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, width, height,
                textureWidth, textureHeight, applyOpacity(tintArgb));
    }

    private static Identifier textureId(String texturePath) {
        String path = texturePath.replace('\\', '/');
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return Identifier.fromNamespaceAndPath(PrimeClient.MOD_ID, path);
    }

    private int applyOpacity(int argb) {
        return drawOpacity >= 0.999f ? argb : ColorUtil.withAlpha(argb, drawOpacity);
    }
}
