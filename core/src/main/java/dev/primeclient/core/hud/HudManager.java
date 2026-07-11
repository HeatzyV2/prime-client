package dev.primeclient.core.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.config.ConfigBinding;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry and renderer of all {@link HudElement}s. */
public final class HudManager implements ConfigBinding {

    private final Map<String, HudElement> byId = new LinkedHashMap<>();
    private HudElement[] renderList = new HudElement[0];
    private JsonObject pendingConfig;

    public <E extends HudElement> E register(E element) {
        HudElement previous = byId.putIfAbsent(element.id(), element);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate HUD element id: " + element.id());
        }
        if (pendingConfig != null) {
            JsonElement section = pendingConfig.get(element.id());
            if (section != null && section.isJsonObject()) {
                readElement(element, section.getAsJsonObject());
            }
        }
        renderList = byId.values().toArray(new HudElement[0]);
        return element;
    }

    public HudElement get(String id) {
        return byId.get(id);
    }

    public Collection<HudElement> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public void render(RenderContext ctx) {
        long now = System.currentTimeMillis();
        int screenWidth = ctx.screenWidth();
        int screenHeight = ctx.screenHeight();
        HudElement[] elements = this.renderList;
        for (int i = 0; i < elements.length; i++) {
            HudElement element = elements[i];
            if (!element.isVisible()) {
                continue;
            }
            float scale = element.scale();
            int localWidth = element.measureWidth(ctx);
            int localHeight = element.measureHeight(ctx);
            float width = localWidth * scale;
            float height = localHeight * scale;
            float x = element.anchor().baseX(screenWidth, width) + element.offsetX();
            float y = element.anchor().baseY(screenHeight, height) + element.offsetY();
            element.setLastBounds(x, y, width, height);

            float pivotX = localWidth / 2f;
            float pivotY = localHeight / 2f;
            ctx.pushTransform(x + width / 2f, y + height / 2f, scale,
                    element.rotation(), pivotX, pivotY);
            ctx.setDrawOpacity(element.opacity());
            element.render(ctx, now);
            ctx.setDrawOpacity(1f);
            ctx.popTransform();
        }
    }

    public HudElement elementAt(double x, double y) {
        HudElement[] elements = this.renderList;
        for (int i = elements.length - 1; i >= 0; i--) {
            if (elements[i].isVisible() && elements[i].containsPoint(x, y)) {
                return elements[i];
            }
        }
        return null;
    }

    @Override
    public String configKey() {
        return "hud";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (HudElement element : byId.values()) {
            JsonObject section = new JsonObject();
            section.addProperty("anchor", element.anchor().name());
            section.addProperty("x", element.offsetX());
            section.addProperty("y", element.offsetY());
            section.addProperty("scale", element.scale());
            section.addProperty("rotation", element.rotation());
            section.addProperty("opacity", element.opacity());
            if (element.tintArgb() != 0) {
                section.addProperty("tint", element.tintArgb());
            }
            json.add(element.id(), section);
        }
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        JsonObject json = element.getAsJsonObject();
        this.pendingConfig = json;
        for (HudElement hudElement : byId.values()) {
            JsonElement section = json.get(hudElement.id());
            if (section != null && section.isJsonObject()) {
                readElement(hudElement, section.getAsJsonObject());
            }
        }
    }

    private static void readElement(HudElement element, JsonObject json) {
        HudAnchor anchor = element.anchor();
        JsonElement anchorJson = json.get("anchor");
        if (anchorJson != null && anchorJson.isJsonPrimitive()) {
            try {
                anchor = HudAnchor.valueOf(anchorJson.getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        float x = json.has("x") ? json.get("x").getAsFloat() : element.offsetX();
        float y = json.has("y") ? json.get("y").getAsFloat() : element.offsetY();
        element.setLayout(anchor, x, y);
        if (json.has("scale")) {
            element.setScale(json.get("scale").getAsFloat());
        }
        if (json.has("rotation")) {
            element.setRotation(json.get("rotation").getAsFloat());
        }
        if (json.has("opacity")) {
            element.setOpacity(json.get("opacity").getAsFloat());
        }
        if (json.has("tint")) {
            element.setTintArgb(json.get("tint").getAsInt());
        }
    }
}
