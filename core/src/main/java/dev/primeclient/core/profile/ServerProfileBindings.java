package dev.primeclient.core.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.primeclient.core.config.ConfigBinding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps normalized server addresses to named config profiles.
 * Applied on world join when Server Auto Profile is enabled.
 */
public final class ServerProfileBindings implements ConfigBinding {

    private final Map<String, String> byServer = new LinkedHashMap<>();

    public String profileForServer(String serverAddress) {
        return byServer.get(normalize(serverAddress));
    }

    public void bind(String serverAddress, String profileName) {
        if (profileName == null || profileName.isBlank()) {
            byServer.remove(normalize(serverAddress));
            return;
        }
        byServer.put(normalize(serverAddress), profileName.trim());
    }

    public void unbind(String serverAddress) {
        byServer.remove(normalize(serverAddress));
    }

    public Map<String, String> all() {
        return Collections.unmodifiableMap(byServer);
    }

    public static String normalize(String server) {
        if (server == null || server.isBlank() || "Singleplayer".equalsIgnoreCase(server)) {
            return "singleplayer";
        }
        return server.trim().toLowerCase().replace(':', '_');
    }

    @Override
    public String configKey() {
        return "server-profiles";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, String> e : byServer.entrySet()) {
            json.addProperty(e.getKey(), e.getValue());
        }
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        byServer.clear();
        if (element == null || !element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : element.getAsJsonObject().entrySet()) {
            JsonElement value = e.getValue();
            if (value instanceof JsonPrimitive primitive && primitive.isString()) {
                byServer.put(e.getKey(), primitive.getAsString());
            }
        }
    }
}
