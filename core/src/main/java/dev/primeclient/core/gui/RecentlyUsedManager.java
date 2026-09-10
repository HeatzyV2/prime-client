package dev.primeclient.core.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Recently opened / configured modules for ClickGUI V3.
 * Most-recent first, capped — not an infinite history.
 */
public final class RecentlyUsedManager {

    public static final int MAX = 12;

    private final ArrayDeque<String> ids = new ArrayDeque<>(MAX);

    public void touch(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return;
        }
        ids.remove(moduleId);
        ids.addFirst(moduleId);
        while (ids.size() > MAX) {
            ids.removeLast();
        }
    }

    public List<Module> resolve(ModuleManager modules) {
        List<Module> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            Module module = modules.get(id);
            if (module != null) {
                out.add(module);
            }
        }
        return out;
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    public JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (String id : ids) {
            array.add(new JsonPrimitive(id));
        }
        return array;
    }

    public void fromJson(JsonElement element) {
        ids.clear();
        if (element == null || !element.isJsonArray()) {
            return;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonPrimitive() || ids.size() >= MAX) {
                continue;
            }
            String id = item.getAsString();
            if (id != null && !id.isBlank() && !ids.contains(id)) {
                ids.addLast(id);
            }
        }
    }
}
