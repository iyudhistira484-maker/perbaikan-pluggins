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

import net.skinsrestorer.shared.utils.ValidationUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ValidationUtilTest {
    @Test
    void validMinecraftUsernames() {
        assertFalse(ValidationUtil.invalidMinecraftUsername("Pistonmaster"));
        assertFalse(ValidationUtil.invalidMinecraftUsername("a"));
        assertFalse(ValidationUtil.invalidMinecraftUsername("Test_User"));
        assertFalse(ValidationUtil.invalidMinecraftUsername("Test-User"));
        assertFalse(ValidationUtil.invalidMinecraftUsername("abc123"));
        assertFalse(ValidationUtil.invalidMinecraftUsername("ABCDEFGHIJKLMNOP")); // 16 chars
        assertFalse(ValidationUtil.invalidMinecraftUsername("09azAZ_-"));
    }

    @Test
    void invalidMinecraftUsernames() {
        assertTrue(ValidationUtil.invalidMinecraftUsername("ABCDEFGHIJKLMNOPQ")); // 17 chars
        assertTrue(ValidationUtil.invalidMinecraftUsername("has space"));
        assertTrue(ValidationUtil.invalidMinecraftUsername("special!char"));
        assertTrue(ValidationUtil.invalidMinecraftUsername("dot.name"));
        assertTrue(ValidationUtil.invalidMinecraftUsername("at@sign"));
        assertTrue(ValidationUtil.invalidMinecraftUsername("hash#tag"));
    }

    @Test
    void validSkinUrls() {
        assertTrue(ValidationUtil.validSkinUrl("https://example.com/skin.png"));
        assertTrue(ValidationUtil.validSkinUrl("http://example.com/skin.png"));
        assertTrue(ValidationUtil.validSkinUrl(ValidationUtil.AXOLOTL_PREFIX + "test"));
    }

    @Test
    void invalidSkinUrls() {
        assertFalse(ValidationUtil.validSkinUrl("ftp://example.com/skin.png"));
        assertFalse(ValidationUtil.validSkinUrl("not-a-url"));
        assertFalse(ValidationUtil.validSkinUrl(""));
    }
}
