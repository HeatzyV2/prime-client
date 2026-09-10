package dev.primeclient.core.cloud;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Optional;

/**
 * Local config backup contract. v1 is an on-disk store under {@code cloud/} on this PC
 * (folder name is historical — not a remote cloud service).
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
