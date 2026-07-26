package com.primeclient.api;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/** Public API for other plugins. */
public interface PrimeClientAPI {

    boolean isPrimeClient(Player player);

    boolean isPrimeClient(UUID uuid);

    Optional<String> getClientVersion(Player player);

    Optional<PrimeProfile> getPrimeProfile(Player player);

    Optional<PrimeProfile> getPrimeProfile(UUID uuid);

    void addPrimeXP(UUID uuid, int amount);

    void addPrimeXP(UUID uuid, int amount, String reason);
}
