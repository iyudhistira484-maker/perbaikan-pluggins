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
package net.skinsrestorer.shared.storage.adapter.file.model.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.storage.model.player.FavouriteData;
import net.skinsrestorer.shared.storage.model.player.HistoryData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class PlayerFile {
    private static final int CURRENT_DATA_VERSION = 2;
    private UUID uniqueId;
    private IdentifierFile skinIdentifier;
    private boolean offlineModeWarningDismissed;
    private List<HistoryFile> history;
    private List<FavouritesFile> favourites;
    private int dataVersion;

    public static PlayerFile create(UUID uniqueId) {
        PlayerFile playerFile = new PlayerFile();
        playerFile.uniqueId = uniqueId;
        playerFile.dataVersion = CURRENT_DATA_VERSION;
        return playerFile;
    }

    public PlayerData toPlayerData() {
        return PlayerData.of(
                uniqueId,
                skinIdentifier == null ? null : skinIdentifier.toIdentifier(),
                offlineModeWarningDismissed
        );
    }

    public void setSkinIdentifier(SkinIdentifier identifier) {
        this.skinIdentifier = IdentifierFile.of(identifier);
    }

    public void setOfflineModeWarningDismissed(boolean offlineModeWarningDismissed) {
        this.offlineModeWarningDismissed = offlineModeWarningDismissed;
        this.dataVersion = CURRENT_DATA_VERSION;
    }

    public List<HistoryData> getHistoryData() {
        return history == null ? List.of() : history.stream().map(HistoryFile::toHistoryData).toList();
    }

    public void setHistoryData(List<HistoryData> historyData) {
        this.history = historyData.stream().map(HistoryFile::of).toList();
    }

    public List<FavouriteData> getFavouritesData() {
        return favourites == null ? List.of() : favourites.stream().map(FavouritesFile::toFavouritesData).toList();
    }

    public void setFavouritesData(List<FavouriteData> favouritesData) {
        this.favourites = favouritesData.stream().map(FavouritesFile::of).toList();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class IdentifierFile {
        private String identifier;
        private SkinVariant skinVariant;
        private SkinType type;

        public static IdentifierFile of(SkinIdentifier identifier) {
            if (identifier == null) {
                return null;
            }

            return new IdentifierFile(identifier.getIdentifier(), identifier.getSkinVariant(), identifier.getSkinType());
        }

        public SkinIdentifier toIdentifier() {
            return SkinIdentifier.of(identifier, skinVariant, type);
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class HistoryFile {
        private long timestamp;
        private IdentifierFile skinIdentifier;

        public static HistoryFile of(HistoryData historyData) {
            if (historyData == null) {
                return null;
            }

            return new HistoryFile(historyData.getTimestamp(), IdentifierFile.of(historyData.getSkinIdentifier()));
        }

        public HistoryData toHistoryData() {
            return HistoryData.of(timestamp, skinIdentifier.toIdentifier());
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class FavouritesFile {
        private long timestamp;
        private IdentifierFile skinIdentifier;

        public static FavouritesFile of(FavouriteData favouriteData) {
            if (favouriteData == null) {
                return null;
            }

            return new FavouritesFile(favouriteData.getTimestamp(), IdentifierFile.of(favouriteData.getSkinIdentifier()));
        }

        public FavouriteData toFavouritesData() {
            return FavouriteData.of(timestamp, skinIdentifier.toIdentifier());
        }
    }
}
