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
package net.skinsrestorer.sound;

import net.skinsrestorer.shared.sound.SoundParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoundParserTest {
    @Test
    void parseNull() {
        assertNull(SoundParser.parse(null));
    }

    @Test
    void parseEmpty() {
        assertNull(SoundParser.parse(""));
    }

    @Test
    void parseNone() {
        assertNull(SoundParser.parse("none"));
        assertNull(SoundParser.parse("NONE"));
    }

    @Test
    void parseSimpleSound() {
        SoundParser.Record record = SoundParser.parse("block.note_block.pling");
        assertNotNull(record);
        assertEquals("block.note_block.pling", record.getSound());
        assertEquals("MASTER", record.getCategory());
        assertEquals(SoundParser.DEFAULT_VOLUME, record.getVolume());
        assertEquals(SoundParser.DEFAULT_PITCH, record.getPitch());
        assertNull(record.getSeed());
    }

    @Test
    void parseWithCategory() {
        SoundParser.Record record = SoundParser.parse("~records@block.note_block.pling");
        assertNotNull(record);
        assertEquals("block.note_block.pling", record.getSound());
        assertEquals("RECORDS", record.getCategory());
    }

    @Test
    void parsePublicSound() {
        SoundParser.Record record = SoundParser.parse("~block.note_block.pling");
        assertNotNull(record);
        assertEquals("block.note_block.pling", record.getSound());
    }

    @Test
    void parseWithVolume() {
        SoundParser.Record record = SoundParser.parse("block.note_block.pling,0.5");
        assertNotNull(record);
        assertEquals(0.5f, record.getVolume());
        assertEquals(SoundParser.DEFAULT_PITCH, record.getPitch());
    }

    @Test
    void parseWithVolumeAndPitch() {
        SoundParser.Record record = SoundParser.parse("block.note_block.pling,0.5,2.0");
        assertNotNull(record);
        assertEquals(0.5f, record.getVolume());
        assertEquals(2.0f, record.getPitch());
    }

    @Test
    void parseWithAllParams() {
        SoundParser.Record record = SoundParser.parse("block.note_block.pling,0.5,2.0,12345");
        assertNotNull(record);
        assertEquals(0.5f, record.getVolume());
        assertEquals(2.0f, record.getPitch());
        assertEquals(12345L, record.getSeed());
    }

    @Test
    void parseCategoryAndAllParams() {
        SoundParser.Record record = SoundParser.parse("~records@block.note_block.pling,0.8,1.5,99");
        assertNotNull(record);
        assertEquals("RECORDS", record.getCategory());
        assertEquals("block.note_block.pling", record.getSound());
        assertEquals(0.8f, record.getVolume());
        assertEquals(1.5f, record.getPitch());
        assertEquals(99L, record.getSeed());
    }

    @Test
    void parseInvalidVolumeThrows() {
        assertThrows(NumberFormatException.class, () -> SoundParser.parse("sound,abc"));
    }

    @Test
    void parseInvalidPitchThrows() {
        assertThrows(NumberFormatException.class, () -> SoundParser.parse("sound,1.0,abc"));
    }

    @Test
    void parseInvalidSeedThrows() {
        assertThrows(NumberFormatException.class, () -> SoundParser.parse("sound,1.0,1.0,abc"));
    }

    @Test
    void parseEmptyNameAfterCategoryThrows() {
        assertThrows(IllegalArgumentException.class, () -> SoundParser.parse("category@"));
    }

    @Test
    void generateSeedWithoutExplicitSeed() {
        SoundParser.Record record = SoundParser.parse("sound");
        assertNotNull(record);
        // Should not throw and should produce a random seed
        record.generateSeed();
    }

    @Test
    void generateSeedWithExplicitSeed() {
        SoundParser.Record record = SoundParser.parse("sound,1,1,42");
        assertNotNull(record);
        assertEquals(42L, record.generateSeed());
    }
}
