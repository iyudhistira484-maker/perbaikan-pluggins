/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.adapter;

import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.storage.HardcodedSkins;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.FavouriteData;
import net.skinsrestorer.shared.storage.model.player.HistoryData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLIndexData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class AdapterHelper {
    private static final String DEFAULT_NAME = "Pistonmaster";
    private static final UUID DEFAULT_UUID = UUID.nameUUIDFromBytes(DEFAULT_NAME.getBytes(StandardCharsets.UTF_8));

    private AdapterHelper() {
    }

    public static void testAdapter(StorageAdapter adapter) {
        UUID playerId = UUID.randomUUID();
        PlayerData playerData = PlayerData.of(playerId, null, true);
        HistoryData historyEntry = HistoryData.of(0, SkinIdentifier.ofCustom("abc"));
        FavouriteData favouriteEntry = FavouriteData.of(0, SkinIdentifier.ofCustom("abc"));

        adapter.setCachedUUID("test", MojangCacheData.of(UUID.randomUUID(), -1));
        adapter.setPlayerData(playerId, playerData);
        adapter.addPlayerHistory(playerId, historyEntry);
        adapter.addPlayerFavourite(playerId, favouriteEntry);
        adapter.setPlayerSkinData(DEFAULT_UUID, PlayerSkinData.of(DEFAULT_UUID, DEFAULT_NAME,
                HardcodedSkins.STEVE.getProperty(), -1));
        adapter.setURLSkinData("test", URLSkinData.of("https://test.com", "test",
                HardcodedSkins.STEVE.getProperty(), SkinVariant.CLASSIC));
        adapter.setCustomSkinData("test-skin", CustomSkinData.of("test-skin",
                null, HardcodedSkins.STEVE.getProperty()));
        adapter.setCustomSkinData("test-skin2", CustomSkinData.of("test-skin2",
                null, HardcodedSkins.ALEX.getProperty()));

        assertEquals(2, adapter.getTotalCustomSkins());
        assertEquals(2, adapter.getCustomGUISkins(0, Integer.MAX_VALUE).size());

        // Check if offset works as well, we actually have two skins in the storage for GUI
        assertEquals(1, adapter.getCustomGUISkins(1, Integer.MAX_VALUE).size());

        assertEquals(1, adapter.getTotalPlayerSkins());
        assertEquals(1, adapter.getPlayerGUISkins(0, Integer.MAX_VALUE).size());

        // Check if offset works as well, we actually have one skins in the storage for GUI
        assertEquals(0, adapter.getPlayerGUISkins(1, Integer.MAX_VALUE).size());

        try {
            assertEquals(playerData, adapter.getPlayerData(playerId).orElseThrow());

            List<HistoryData> history = adapter.getPlayerHistory(playerId, 0, 10);
            assertEquals(1, history.size());
            assertEquals(historyEntry, history.getFirst());

            List<FavouriteData> favourites = adapter.getPlayerFavourites(playerId, 0, 10);
            assertEquals(1, favourites.size());
            assertEquals(favouriteEntry, favourites.getFirst());

            testNonExistentData(adapter);
            testCacheReadBack(adapter);
            testPlayerSkinDataCrud(adapter);
            testUrlSkinDataCrud(adapter);
            testUrlIndexCrud(adapter);
            testCustomSkinDataCrud(adapter);
            testHistoryCountRemoveTrim(adapter, playerId);
            testFavouriteCountRemoveTrim(adapter, playerId);
            testCooldownsCrud(adapter);
            testPurgeOldSkins(adapter);
            testOverwriteBehavior(adapter);
        } catch (StorageAdapter.StorageException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testNonExistentData(StorageAdapter adapter) throws StorageAdapter.StorageException {
        UUID nonExistent = UUID.randomUUID();
        assertTrue(adapter.getPlayerData(nonExistent).isEmpty());
        assertTrue(adapter.getPlayerSkinData(nonExistent).isEmpty());
        assertTrue(adapter.getURLSkinData("nonexistent-url", SkinVariant.CLASSIC).isEmpty());
        assertTrue(adapter.getCustomSkinData("nonexistent-skin").isEmpty());
        assertTrue(adapter.getCachedUUID("nonexistent-player").isEmpty());
        assertTrue(adapter.getURLSkinIndex("nonexistent-url").isEmpty());
        assertEquals(0, adapter.getPlayerHistoryCount(nonExistent));
        assertEquals(0, adapter.getPlayerFavouriteCount(nonExistent));
        assertTrue(adapter.getPlayerHistory(nonExistent, 0, 10).isEmpty());
        assertTrue(adapter.getPlayerFavourites(nonExistent, 0, 10).isEmpty());
        assertTrue(adapter.getCooldowns(nonExistent).isEmpty());
    }

    private static void testCacheReadBack(StorageAdapter adapter) throws StorageAdapter.StorageException {
        UUID cachedUuid = UUID.randomUUID();
        long cachedTimestamp = 12345L;
        adapter.setCachedUUID("cache-test", MojangCacheData.of(cachedUuid, cachedTimestamp));

        MojangCacheData cached = adapter.getCachedUUID("cache-test").orElseThrow();
        assertEquals(cachedUuid, cached.getUniqueId().orElseThrow());
        assertEquals(cachedTimestamp, cached.getTimestamp());
    }

    private static void testPlayerSkinDataCrud(StorageAdapter adapter) throws StorageAdapter.StorageException {
        UUID skinUuid = UUID.randomUUID();
        PlayerSkinData skinData = PlayerSkinData.of(skinUuid, "TestPlayer",
                HardcodedSkins.STEVE.getProperty(), 5000L);

        adapter.setPlayerSkinData(skinUuid, skinData);

        PlayerSkinData retrieved = adapter.getPlayerSkinData(skinUuid).orElseThrow();
        assertEquals(skinUuid, retrieved.getUniqueId());
        assertEquals("TestPlayer", retrieved.getLastKnownName());
        assertEquals(HardcodedSkins.STEVE.getProperty(), retrieved.getProperty());
        assertEquals(5000L, retrieved.getTimestamp());

        adapter.removePlayerSkinData(skinUuid);
        assertTrue(adapter.getPlayerSkinData(skinUuid).isEmpty());
    }

    private static void testUrlSkinDataCrud(StorageAdapter adapter) throws StorageAdapter.StorageException {
        String url = "https://url-crud-test.com/skin.png";
        URLSkinData urlData = URLSkinData.of(url, "mineskin-id",
                HardcodedSkins.ALEX.getProperty(), SkinVariant.SLIM);

        adapter.setURLSkinData(url, urlData);

        URLSkinData retrieved = adapter.getURLSkinData(url, SkinVariant.SLIM).orElseThrow();
        assertEquals(url, retrieved.getUrl());
        assertEquals("mineskin-id", retrieved.getMineSkinId());
        assertEquals(HardcodedSkins.ALEX.getProperty(), retrieved.getProperty());
        assertEquals(SkinVariant.SLIM, retrieved.getSkinVariant());

        // Different variant should not find it
        assertTrue(adapter.getURLSkinData(url, SkinVariant.CLASSIC).isEmpty());

        adapter.removeURLSkinData(url, SkinVariant.SLIM);
        assertTrue(adapter.getURLSkinData(url, SkinVariant.SLIM).isEmpty());
    }

    private static void testUrlIndexCrud(StorageAdapter adapter) throws StorageAdapter.StorageException {
        String url = "https://url-index-test.com/skin.png";
        URLIndexData indexData = URLIndexData.of(url, SkinVariant.CLASSIC);

        adapter.setURLSkinIndex(url, indexData);

        URLIndexData retrieved = adapter.getURLSkinIndex(url).orElseThrow();
        assertEquals(url, retrieved.getUrl());
        assertEquals(SkinVariant.CLASSIC, retrieved.getSkinVariant());

        adapter.removeURLSkinIndex(url);
        assertTrue(adapter.getURLSkinIndex(url).isEmpty());
    }

    private static void testCustomSkinDataCrud(StorageAdapter adapter) throws StorageAdapter.StorageException {
        String skinName = "crud-test-skin";
        CustomSkinData customData = CustomSkinData.of(skinName, null,
                HardcodedSkins.STEVE.getProperty());

        adapter.setCustomSkinData(skinName, customData);
        int countAfterAdd = adapter.getTotalCustomSkins();

        CustomSkinData retrieved = adapter.getCustomSkinData(skinName).orElseThrow();
        assertEquals(skinName, retrieved.getSkinName());
        assertNull(retrieved.getDisplayName());
        assertEquals(HardcodedSkins.STEVE.getProperty(), retrieved.getProperty());

        adapter.removeCustomSkinData(skinName);
        assertTrue(adapter.getCustomSkinData(skinName).isEmpty());
        assertEquals(countAfterAdd - 1, adapter.getTotalCustomSkins());
    }

    private static void testHistoryCountRemoveTrim(StorageAdapter adapter, UUID playerId) throws StorageAdapter.StorageException {
        // Already have 1 history entry from the main test
        assertEquals(1, adapter.getPlayerHistoryCount(playerId));

        // Add more entries with different timestamps
        adapter.addPlayerHistory(playerId, HistoryData.of(100, SkinIdentifier.ofCustom("hist2")));
        adapter.addPlayerHistory(playerId, HistoryData.of(200, SkinIdentifier.ofCustom("hist3")));
        assertEquals(3, adapter.getPlayerHistoryCount(playerId));

        // Verify pagination
        List<HistoryData> page = adapter.getPlayerHistory(playerId, 0, 2);
        assertEquals(2, page.size());
        List<HistoryData> page2 = adapter.getPlayerHistory(playerId, 2, 2);
        assertEquals(1, page2.size());

        // Remove by timestamp
        adapter.removePlayerHistory(playerId, 100);
        assertEquals(2, adapter.getPlayerHistoryCount(playerId));

        // Trim to keep only 1
        adapter.trimPlayerHistory(playerId, 1);
        assertEquals(1, adapter.getPlayerHistoryCount(playerId));
    }

    private static void testFavouriteCountRemoveTrim(StorageAdapter adapter, UUID playerId) throws StorageAdapter.StorageException {
        // Already have 1 favourite entry from the main test
        assertEquals(1, adapter.getPlayerFavouriteCount(playerId));

        // Add more entries with different skin identifiers
        adapter.addPlayerFavourite(playerId, FavouriteData.of(100, SkinIdentifier.ofCustom("fav2")));
        adapter.addPlayerFavourite(playerId, FavouriteData.of(200, SkinIdentifier.ofCustom("fav3")));
        assertEquals(3, adapter.getPlayerFavouriteCount(playerId));

        // Verify pagination
        List<FavouriteData> page = adapter.getPlayerFavourites(playerId, 0, 2);
        assertEquals(2, page.size());
        List<FavouriteData> page2 = adapter.getPlayerFavourites(playerId, 2, 2);
        assertEquals(1, page2.size());

        // Remove by skin identifier
        adapter.removePlayerFavourite(playerId, SkinIdentifier.ofCustom("fav2"));
        assertEquals(2, adapter.getPlayerFavouriteCount(playerId));

        // Trim to keep only 1
        adapter.trimPlayerFavourites(playerId, 1);
        assertEquals(1, adapter.getPlayerFavouriteCount(playerId));
    }

    private static void testCooldownsCrud(StorageAdapter adapter) throws StorageAdapter.StorageException {
        UUID cooldownOwner = UUID.randomUUID();
        Instant now = Instant.now();
        Duration fiveMinutes = Duration.ofMinutes(5);
        Duration tenMinutes = Duration.ofMinutes(10);

        adapter.setCooldown(cooldownOwner, "group1", now, fiveMinutes);
        adapter.setCooldown(cooldownOwner, "group2", now, tenMinutes);

        List<UUID> profiles = adapter.getAllCooldownProfiles();
        assertTrue(profiles.contains(cooldownOwner));

        List<StorageAdapter.StorageCooldown> cooldowns = adapter.getCooldowns(cooldownOwner);
        assertEquals(2, cooldowns.size());

        // Remove one cooldown
        adapter.removeCooldown(cooldownOwner, "group1");
        cooldowns = adapter.getCooldowns(cooldownOwner);
        assertEquals(1, cooldowns.size());
        assertEquals("group2", cooldowns.getFirst().groupName());

        // Remove remaining cooldown
        adapter.removeCooldown(cooldownOwner, "group2");
        assertTrue(adapter.getCooldowns(cooldownOwner).isEmpty());
    }

    private static void testPurgeOldSkins(StorageAdapter adapter) throws StorageAdapter.StorageException {
        UUID oldSkinUuid = UUID.randomUUID();
        UUID recentSkinUuid = UUID.randomUUID();

        // Old skin with timestamp 100
        adapter.setPlayerSkinData(oldSkinUuid, PlayerSkinData.of(oldSkinUuid, "OldPlayer",
                HardcodedSkins.STEVE.getProperty(), 100L));
        // Recent skin with a large timestamp
        adapter.setPlayerSkinData(recentSkinUuid, PlayerSkinData.of(recentSkinUuid, "RecentPlayer",
                HardcodedSkins.ALEX.getProperty(), 999999999L));

        // Purge skins with timestamp <= 500
        adapter.purgeStoredOldSkins(500L);

        // Old skin should be purged
        assertTrue(adapter.getPlayerSkinData(oldSkinUuid).isEmpty());
        // Recent skin should survive
        assertTrue(adapter.getPlayerSkinData(recentSkinUuid).isPresent());

        // Clean up
        adapter.removePlayerSkinData(recentSkinUuid);
    }

    private static void testOverwriteBehavior(StorageAdapter adapter) throws StorageAdapter.StorageException {
        // Player data overwrite
        UUID overwriteId = UUID.randomUUID();
        adapter.setPlayerData(overwriteId, PlayerData.of(overwriteId, null));
        SkinIdentifier newSkin = SkinIdentifier.ofCustom("overwritten");
        adapter.setPlayerData(overwriteId, PlayerData.of(overwriteId, newSkin));
        PlayerData retrieved = adapter.getPlayerData(overwriteId).orElseThrow();
        assertEquals(newSkin, retrieved.getSkinIdentifier());

        // Custom skin data overwrite
        adapter.setCustomSkinData("overwrite-skin", CustomSkinData.of("overwrite-skin",
                null, HardcodedSkins.STEVE.getProperty()));
        adapter.setCustomSkinData("overwrite-skin", CustomSkinData.of("overwrite-skin",
                null, HardcodedSkins.ALEX.getProperty()));
        CustomSkinData customRetrieved = adapter.getCustomSkinData("overwrite-skin").orElseThrow();
        assertEquals(HardcodedSkins.ALEX.getProperty(), customRetrieved.getProperty());

        // Cache overwrite
        UUID firstUuid = UUID.randomUUID();
        UUID secondUuid = UUID.randomUUID();
        adapter.setCachedUUID("overwrite-cache", MojangCacheData.of(firstUuid, 1L));
        adapter.setCachedUUID("overwrite-cache", MojangCacheData.of(secondUuid, 2L));
        MojangCacheData cacheRetrieved = adapter.getCachedUUID("overwrite-cache").orElseThrow();
        assertEquals(secondUuid, cacheRetrieved.getUniqueId().orElseThrow());
        assertEquals(2L, cacheRetrieved.getTimestamp());

        // Clean up
        adapter.removeCustomSkinData("overwrite-skin");
    }
}
