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
package net.skinsrestorer.log;

import net.skinsrestorer.shared.log.ANSIConverter;
import net.skinsrestorer.shared.log.SRChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ANSIConverterTest {
    @Test
    void convertGreenColor() {
        String input = SRChatColor.GREEN + "Hello";
        String result = ANSIConverter.convertToAnsi(input);
        assertTrue(result.contains("\u001B[32;1m"));
        assertTrue(result.contains("Hello"));
    }

    @Test
    void convertReset() {
        String input = SRChatColor.RESET + "plain";
        String result = ANSIConverter.convertToAnsi(input);
        assertTrue(result.contains("\u001B[0;39m"));
        assertTrue(result.contains("plain"));
    }

    @Test
    void convertMultipleColors() {
        String input = SRChatColor.RED + "error " + SRChatColor.GREEN + "ok";
        String result = ANSIConverter.convertToAnsi(input);
        assertTrue(result.contains("\u001B[31;1m"));
        assertTrue(result.contains("\u001B[32;1m"));
        assertFalse(result.contains(SRChatColor.COLOR_CHAR + ""));
    }

    @Test
    void plainTextUnchanged() {
        assertEquals("Hello World", ANSIConverter.convertToAnsi("Hello World"));
    }
}
