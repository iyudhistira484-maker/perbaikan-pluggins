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

import net.skinsrestorer.shared.utils.UUIDUtils;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UUIDUtilsTest {
    private static final UUID TEST_UUID = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    void tryParseUniqueIdDashed() {
        assertEquals(TEST_UUID, UUIDUtils.tryParseUniqueId("069a79f4-44e9-4726-a5be-fca90e38aaf5").orElseThrow());
    }

    @Test
    void tryParseUniqueIdNoDashes() {
        assertEquals(TEST_UUID, UUIDUtils.tryParseUniqueId("069a79f444e94726a5befca90e38aaf5").orElseThrow());
    }

    @Test
    void tryParseUniqueIdInvalid() {
        assertTrue(UUIDUtils.tryParseUniqueId("not-a-uuid").isEmpty());
        assertTrue(UUIDUtils.tryParseUniqueId("").isEmpty());
        assertTrue(UUIDUtils.tryParseUniqueId("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz").isEmpty()); // 32 chars but invalid hex
    }

    @Test
    void parseUniqueIdNullable() {
        assertEquals(TEST_UUID, UUIDUtils.parseUniqueIdNullable("069a79f4-44e9-4726-a5be-fca90e38aaf5"));
        assertNull(UUIDUtils.parseUniqueIdNullable("not-a-uuid"));
    }

    @Test
    void convertToDashed() {
        assertEquals(TEST_UUID, UUIDUtils.convertToDashed("069a79f444e94726a5befca90e38aaf5"));
    }

    @Test
    void convertToNoDashes() {
        assertEquals("069a79f444e94726a5befca90e38aaf5", UUIDUtils.convertToNoDashes(TEST_UUID));
    }

    @Test
    void roundTrip() {
        UUID original = UUID.randomUUID();
        String noDashes = UUIDUtils.convertToNoDashes(original);
        UUID restored = UUIDUtils.convertToDashed(noDashes);
        assertEquals(original, restored);
    }
}
