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
package net.skinsrestorer.utils;

import net.skinsrestorer.shared.utils.ExpiringSet;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExpiringSetTest {
    @Test
    void addAndContains() {
        ExpiringSet<String> set = new ExpiringSet<>(60, TimeUnit.SECONDS);
        set.add("item");
        assertTrue(set.contains("item"));
    }

    @Test
    void doesNotContainUnadded() {
        ExpiringSet<String> set = new ExpiringSet<>(60, TimeUnit.SECONDS);
        assertFalse(set.contains("missing"));
    }

    @Test
    void tracksDifferentItems() {
        ExpiringSet<String> set = new ExpiringSet<>(60, TimeUnit.SECONDS);
        set.add("a");
        set.add("b");
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertFalse(set.contains("c"));
    }
}
