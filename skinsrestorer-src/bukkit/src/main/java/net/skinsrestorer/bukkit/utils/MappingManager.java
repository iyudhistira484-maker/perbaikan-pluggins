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
package net.skinsrestorer.bukkit.utils;

import net.lenni0451.reflect.stream.RStream;
import net.skinsrestorer.bukkit.mappings.*;
import net.skinsrestorer.bukkit.paper.PaperUtil;
import org.bukkit.Server;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class MappingManager {
    private static final List<IMapping> MAPPINGS = List.of(
            new Mapping1_18(),
            new Mapping1_18_2(),
            new Mapping1_19(),
            new Mapping1_19_1(),
            new Mapping1_19_2(),
            new Mapping1_19_3(),
            new Mapping1_19_4(),
            new Mapping1_20(),
            new Mapping1_20_2(),
            new Mapping1_20_4(),
            new Mapping1_20_5(),
            new Mapping1_21(),
            new Mapping1_21_2(),
            new Mapping1_21_4(),
            new Mapping1_21_5(),
            new Mapping1_21_6(),
            new Mapping1_21_9(),
            new PaperMapping1_21_10(),
            new PaperMapping1_21_11(),
            new SpigotMapping1_21_11(),
            new PaperMapping26_1(),
            new SpigotMapping26_1()
    );

    public static Optional<IMapping> getMapping(Server server) {
        Optional<String> spigotApiVersion = getSpigotApiVersion();
        Optional<String> spigotMappingVersion = getSpigotMappingVersion(server);
        Optional<String> paperMinecraftVersionId = getPaperMinecraftVersionId();

        if (spigotApiVersion.isEmpty() && spigotMappingVersion.isEmpty() && paperMinecraftVersionId.isEmpty()) {
            return Optional.empty();
        }

        Predicate<IMapping> isSupportedSpigotApiVersion = mapping -> spigotApiVersion
                .map(mapping::supportsSpigotApiVersion)
                .orElse(false);
        Predicate<IMapping> isSupportedSpigotMapping = mapping -> spigotMappingVersion
                .map(mapping.getSpigotMappingVersions()::contains)
                .orElse(false);
        Predicate<IMapping> isSupportedPaperMapping = mapping -> paperMinecraftVersionId
                .map(mapping::supportsPaperMinecraftVersionId)
                .orElse(false);

        return MAPPINGS.stream()
                .filter(mapping -> isSupportedSpigotApiVersion.test(mapping) // Needs to be before mapping because mapping for 26.1 & 26.2 is the same
                        || isSupportedSpigotMapping.test(mapping)
                        || isSupportedPaperMapping.test(mapping))
                .findFirst();
    }

    @SuppressWarnings({"deprecation"})
    public static Optional<String> getSpigotMappingVersion(Server server) {
        var craftMagicNumbers = server.getUnsafe();
        try {
            Method method = craftMagicNumbers.getClass().getMethod("getMappingsVersion");
            return ((String) method.invoke(craftMagicNumbers, new Object[0])).describeConstable();
        } catch (ReflectiveOperationException e) {
            // Happens on paper >= 1.21.6
            return Optional.empty();
        }
    }

    public static Optional<String> getSpigotApiVersion() {
        try {
            return Optional.of(RStream.of("org.bukkit.craftbukkit.util.ApiVersion")
                    .fields()
                    .filterStatic(true)
                    .by("CURRENT")
                    .stream()
                    .methods()
                    .by("getVersionString")
                    .invoke());
        } catch (Throwable t) {
            // Happens on older spigot
            return Optional.empty();
        }
    }

    public static Optional<String> getPaperMinecraftVersionId() {
        return PaperUtil.getMinecraftVersionId();
    }

    private MappingManager() {
    }
}
