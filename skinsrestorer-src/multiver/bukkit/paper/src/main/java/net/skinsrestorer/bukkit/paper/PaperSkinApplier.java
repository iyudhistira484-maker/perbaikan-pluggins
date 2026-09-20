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
package net.skinsrestorer.bukkit.paper;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.entity.Player;

public final class PaperSkinApplier {
    private PaperSkinApplier() {
    }

    public static boolean hasProfileMethod() {
        try {
            Player.class.getMethod("setPlayerProfile", PlayerProfile.class);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static void applySkin(Player player, SkinProperty property) {
        PlayerProfile profile = player.getPlayerProfile();

        profile.getProperties().removeIf(profileProperty -> SkinProperty.TEXTURES_NAME.equals(profileProperty.getName()));
        profile.getProperties().add(new ProfileProperty(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));

        player.setPlayerProfile(profile);

        // Update the player health and food, does not work on older versions
        try {
            player.sendHealthUpdate();
        } catch (NoSuchMethodError ignored) {
        }
    }
}
