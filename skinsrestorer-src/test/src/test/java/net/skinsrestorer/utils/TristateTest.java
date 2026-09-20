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

import net.skinsrestorer.shared.utils.Tristate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TristateTest {
    @Test
    void fromBooleanTrue() {
        assertEquals(Tristate.TRUE, Tristate.fromBoolean(true));
    }

    @Test
    void fromBooleanFalse() {
        assertEquals(Tristate.FALSE, Tristate.fromBoolean(false));
    }

    @Test
    void asBooleanTrue() {
        assertTrue(Tristate.TRUE.asBoolean());
    }

    @Test
    void asBooleanFalse() {
        assertFalse(Tristate.FALSE.asBoolean());
    }

    @Test
    void asBooleanUndefined() {
        assertFalse(Tristate.UNDEFINED.asBoolean());
    }
}
