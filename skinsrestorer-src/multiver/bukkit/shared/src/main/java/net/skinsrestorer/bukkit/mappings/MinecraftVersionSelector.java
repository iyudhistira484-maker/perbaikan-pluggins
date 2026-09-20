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

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MinecraftVersionSelector {
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");
    private static final Pattern RANGE_TOKEN_PATTERN = Pattern.compile("(<=|>=|<|>|=)(\\d+(?:\\.\\d+)*)");

    static boolean matches(String selector, String versionId) {
        String normalizedSelector = selector.trim();

        if (normalizedSelector.isEmpty()) {
            throw invalidSelector(selector);
        }

        if (normalizedSelector.endsWith("+")) {
            return matchesPrefix(normalizedSelector.substring(0, normalizedSelector.length() - 1), versionId, true, selector);
        }

        if (normalizedSelector.endsWith(".*")) {
            return matchesPrefix(normalizedSelector.substring(0, normalizedSelector.length() - 2), versionId, false, selector);
        }

        if (startsWithComparator(normalizedSelector)) {
            return matchesRange(normalizedSelector, versionId, selector);
        }

        return normalizedSelector.equals(versionId);
    }

    private static boolean startsWithComparator(String selector) {
        return selector.startsWith(">") || selector.startsWith("<") || selector.startsWith("=");
    }

    private static boolean matchesPrefix(String prefix, String versionId, boolean includeExact, String selector) {
        ParsedVersion parsedPrefix = ParsedVersion.parse(prefix, selector);
        return ParsedVersion.tryParse(versionId)
                .filter(version -> version.startsWith(parsedPrefix, includeExact))
                .isPresent();
    }

    private static boolean matchesRange(String selector, String versionId, String rawSelector) {
        Optional<ParsedVersion> parsedVersion = ParsedVersion.tryParse(versionId);
        if (parsedVersion.isEmpty()) {
            return false;
        }

        for (String token : selector.split("\\s+")) {
            Matcher matcher = RANGE_TOKEN_PATTERN.matcher(token);
            if (!matcher.matches()) {
                throw invalidSelector(rawSelector);
            }

            ParsedVersion boundary = ParsedVersion.parse(matcher.group(2), rawSelector);
            int comparison = parsedVersion.get().compareTo(boundary);

            switch (matcher.group(1)) {
                case ">" -> {
                    if (comparison <= 0) {
                        return false;
                    }
                }
                case ">=" -> {
                    if (comparison < 0) {
                        return false;
                    }
                }
                case "<" -> {
                    if (comparison >= 0) {
                        return false;
                    }
                }
                case "<=" -> {
                    if (comparison > 0) {
                        return false;
                    }
                }
                case "=" -> {
                    if (comparison != 0) {
                        return false;
                    }
                }
                default -> throw invalidSelector(rawSelector);
            }
        }

        return true;
    }

    private static IllegalArgumentException invalidSelector(String selector) {
        return new IllegalArgumentException("Invalid Minecraft version selector: " + selector);
    }

    private MinecraftVersionSelector() {
    }

    private record ParsedVersion(int[] parts) {
        private static ParsedVersion parse(String version, String selector) {
            return tryParse(version)
                    .orElseThrow(() -> invalidSelector(selector));
        }

        private static Optional<ParsedVersion> tryParse(String version) {
            if (!VERSION_PATTERN.matcher(version).matches()) {
                return Optional.empty();
            }

            return Optional.of(new ParsedVersion(Arrays.stream(version.split("\\."))
                    .mapToInt(Integer::parseInt)
                    .toArray()));
        }

        private boolean startsWith(ParsedVersion prefix, boolean includeExact) {
            if (parts.length < prefix.parts.length || (!includeExact && parts.length == prefix.parts.length)) {
                return false;
            }

            for (int i = 0; i < prefix.parts.length; i++) {
                if (parts[i] != prefix.parts[i]) {
                    return false;
                }
            }

            return true;
        }

        private int compareTo(ParsedVersion other) {
            int length = Math.max(parts.length, other.parts.length);
            for (int i = 0; i < length; i++) {
                int left = i < parts.length ? parts[i] : 0;
                int right = i < other.parts.length ? other.parts[i] : 0;

                if (left != right) {
                    return Integer.compare(left, right);
                }
            }

            return 0;
        }
    }
}
