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
package net.skinsrestorer.bukkit.wrapper;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import org.bukkit.Material;

public final class BukkitComponentHelper {
    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = createLegacySerializer();

    public static BaseComponent[] deserialize(ComponentString messageJson) {
        return ComponentSerializer.parse(ComponentHelper.convertJsonToCompatibilityJson(messageJson));
    }

    public static String toLegacy(ComponentString messageJson) {
        return LEGACY_SERIALIZER.serialize(GSON_SERIALIZER.deserialize(messageJson.jsonString()));
    }

    private static LegacyComponentSerializer createLegacySerializer() {
        LegacyComponentSerializer.Builder builder = LegacyComponentSerializer.builder()
                .character(LegacyComponentSerializer.SECTION_CHAR);
        if (Material.getMaterial("NETHERITE_PICKAXE") != null) {
            builder.hexColors().useUnusualXRepeatedCharacterHexFormat();
        }

        return builder.build();
    }

    private BukkitComponentHelper() {
    }
}
