package dev.primeclient.core.cosmetics;

import dev.primeclient.core.state.CosmeticsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmeticManagerTest {

    @AfterEach
    void tearDown() {
        CosmeticsState.reset();
    }

    @Test
    void catalogIncludesFullCosmeticsUpdate() {
        CosmeticManager manager = new CosmeticManager();
        assertTrue(manager.catalog().containsKey("cape-prime-classic"));
        assertTrue(manager.catalog().containsKey("wings-inferno"));
        assertTrue(manager.catalog().containsKey("aura-fire"));
        assertTrue(manager.catalog().containsKey("trail-prime"));
        assertTrue(manager.catalog().containsKey("hat-crown"));
        assertTrue(manager.catalog().containsKey("emote-wave"));
        assertTrue(manager.catalog().containsKey("badge-founder"));
    }

    @Test
    void equipUpdatesLocalState() {
        CosmeticManager manager = new CosmeticManager();
        manager.equip(CosmeticType.CAPE, "cape-crimson");
        manager.equip(CosmeticType.WINGS, "wings-ember");
        manager.equip(CosmeticType.HAT, "hat-crown");
        manager.equip(CosmeticType.AURA, "aura-fire");
        assertEquals("cape-crimson", CosmeticsState.localCapeId());
        assertEquals("wings-ember", CosmeticsState.localWingsId());
        assertEquals("hat-crown", CosmeticsState.localHatId());
        assertEquals("aura-fire", CosmeticsState.localAuraId());
    }

    @Test
    void legacyWingsLightAliasesAurora() {
        CosmeticManager manager = new CosmeticManager();
        manager.equip(CosmeticType.WINGS, "wings-light");
        assertEquals("wings-aurora", manager.equipped(CosmeticType.WINGS).id());
    }

    @Test
    void legacyCapePrimeAliasesClassic() {
        CosmeticManager manager = new CosmeticManager();
        manager.equip(CosmeticType.CAPE, "cape-prime");
        assertNotNull(manager.equipped(CosmeticType.CAPE));
        assertTrue(CosmeticTextures.isKnownCape("cape-prime"));
    }

    @Test
    void favoritesAndCollection() {
        CosmeticManager manager = new CosmeticManager();
        manager.toggleFavorite("hat-crown");
        assertTrue(manager.isFavorite("hat-crown"));
        manager.toggleFavorite("hat-crown");
        assertFalse(manager.isFavorite("hat-crown"));
        CollectionProgress progress = manager.collectionProgress();
        assertTrue(progress.catalogTotal() > 20);
        assertTrue(progress.ownedTotal() >= progress.catalogTotal() - 1);
    }

    @Test
    void peerLoadoutIsSeparateFromLocal() {
        UUID peer = UUID.randomUUID();
        CosmeticsState.setLocalLoadout(new CosmeticLoadout(
                "cape-prime-classic", "wings-aurora", "aura-fire", "trail-prime", "hat-crown", "badge-founder"));
        CosmeticsState.setPeerLoadout(peer, new CosmeticLoadout(
                "cape-star", "wings-ember", "aura-void", "trail-flame", "hat-horns", "badge-partner"));
        assertEquals("cape-prime-classic", CosmeticsState.loadoutFor(peer, true).capeId());
        assertEquals("cape-star", CosmeticsState.loadoutFor(peer, false).capeId());
        assertEquals("wings-ember", CosmeticsState.loadoutFor(peer, false).wingsId());
        assertEquals("hat-horns", CosmeticsState.loadoutFor(peer, false).hatId());
    }
}
