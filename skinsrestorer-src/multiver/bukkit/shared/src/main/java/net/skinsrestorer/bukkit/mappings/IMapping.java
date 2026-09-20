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
package net.skinsrestorer.bukkit.mappings;

import net.skinsrestorer.viaversion.ViaPacketData;
import net.skinsrestorer.viaversion.ViaRefreshProvider;
import org.bukkit.entity.Player;

import java.util.Set;

public interface IMapping {
    void accept(Player player, ViaRefreshProvider viaFunction);

    void resendInfoPackets(Player toResend, Player toSendTo);

    /**
     * Format as in ServerBuildInfo#minecraftVersionId.
     * Supports exact versions and selectors like {@code 26.1.*}, {@code 26.1+}, and {@code >=26.1 <26.2}.
     *
     * @return The supported paper minecraft version ids or selectors of the mapping
     */
    default Set<String> getPaperMinecraftVersionIds() {
        return Set.of();
    }

    default boolean supportsPaperMinecraftVersionId(String versionId) {
        return getPaperMinecraftVersionIds().stream()
                .anyMatch(selector -> MinecraftVersionSelector.matches(selector, versionId));
    }

    /**
     * Can be found at <a href="https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/util/CraftMagicNumbers.java">SpigotMC</a>
     *
     * @return The supported spigot mapping versions of the mapping
     */
    default Set<String> getSpigotMappingVersions() {
        return Set.of();
    }

    /**
     * Format as in ApiVersion#getVersionString.
     * Supports exact versions and selectors like {@code 26.1.*}, {@code 26.1+}, and {@code >=26.1 <26.2}.
     *
     * @return The supported spigot api versions
     */
    default Set<String> getSpigotApiVersions() {
        return Set.of();
    }

    default boolean supportsSpigotApiVersion(String versionId) {
        return getSpigotApiVersions().stream()
                .anyMatch(selector -> MinecraftVersionSelector.matches(selector, versionId));
    }

    @SuppressWarnings("deprecation")
    static ViaPacketData newViaPacketData(Player player, long seed, int gamemodeId, boolean isFlat) {
        return new ViaPacketData(player.getUniqueId(), player.getWorld().getEnvironment().getId(), seed, gamemodeId, isFlat);
    }
}
