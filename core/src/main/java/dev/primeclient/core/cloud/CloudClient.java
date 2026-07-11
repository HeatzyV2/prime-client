package dev.primeclient.core.cloud;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Optional;

/**
 * Remote config sync contract. v1.1 ships a local stub; swap implementation for real API later.
 */
public interface CloudClient {

    record VersionEntry(String id, String label, long timestampMillis) {
    }

    boolean isAuthenticated();

    Optional<String> accountId();

    void uploadConfig(String profileName, JsonElement config);

    Optional<JsonElement> downloadConfig(String profileName);

    List<VersionEntry> listVersions(String profileName);

    Optional<JsonElement> restoreVersion(String profileName, String versionId);
}
