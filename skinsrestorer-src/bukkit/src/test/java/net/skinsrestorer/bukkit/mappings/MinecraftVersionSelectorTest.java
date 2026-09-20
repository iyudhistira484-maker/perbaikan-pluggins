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

import net.skinsrestorer.viaversion.ViaRefreshProvider;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftVersionSelectorTest {
    @Test
    void exactSelectorMatchesOnlyExactVersion() {
        assertTrue(MinecraftVersionSelector.matches("26.1", "26.1"));
        assertFalse(MinecraftVersionSelector.matches("26.1", "26.1.1"));
    }

    @Test
    void wildcardSelectorMatchesOnlyChildren() {
        assertFalse(MinecraftVersionSelector.matches("26.1.*", "26.1"));
        assertTrue(MinecraftVersionSelector.matches("26.1.*", "26.1.1"));
        assertTrue(MinecraftVersionSelector.matches("26.1.*", "26.1.2"));
        assertFalse(MinecraftVersionSelector.matches("26.1.*", "26.2"));
    }

    @Test
    void plusSelectorMatchesBaseAndChildren() {
        assertTrue(MinecraftVersionSelector.matches("26.1+", "26.1"));
        assertTrue(MinecraftVersionSelector.matches("26.1+", "26.1.1"));
        assertTrue(MinecraftVersionSelector.matches("26.1+", "26.1.2"));
        assertFalse(MinecraftVersionSelector.matches("26.1+", "26.10"));
        assertFalse(MinecraftVersionSelector.matches("26.1+", "26.11"));
        assertFalse(MinecraftVersionSelector.matches("26.1+", "26.2"));
    }

    @Test
    void rangeSelectorMatchesBoundedVersions() {
        assertTrue(MinecraftVersionSelector.matches(">=1.21.6 <1.21.9", "1.21.6"));
        assertTrue(MinecraftVersionSelector.matches(">=1.21.6 <1.21.9", "1.21.8"));
        assertFalse(MinecraftVersionSelector.matches(">=1.21.6 <1.21.9", "1.21.9"));
    }

    @Test
    void mappingSupportsAnyMatchingSelector() {
        IMapping mapping = new TestMapping(Set.of("26.1+", ">=1.21.6 <1.21.9"));

        assertTrue(mapping.supportsPaperMinecraftVersionId("26.1.2"));
        assertTrue(mapping.supportsPaperMinecraftVersionId("1.21.8"));
        assertFalse(mapping.supportsPaperMinecraftVersionId("1.21.9"));
    }

    @Test
    void invalidSelectorsFailFast() {
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersionSelector.matches("26.1..*", "26.1.1"));
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersionSelector.matches(">=1.21.6 1.21.9", "1.21.8"));
    }

    private record TestMapping(Set<String> selectors) implements IMapping {
        @Override
        public void accept(Player player, ViaRefreshProvider viaFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void resendInfoPackets(Player toResend, Player toSendTo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> getPaperMinecraftVersionIds() {
            return selectors;
        }

    }
}
