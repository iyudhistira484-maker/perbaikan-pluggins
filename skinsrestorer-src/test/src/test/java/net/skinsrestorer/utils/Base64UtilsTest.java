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

import net.skinsrestorer.api.Base64Utils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class Base64UtilsTest {
    @Test
    void encodeDecodeRoundTrip() {
        String original = "Hello, World!";
        String encoded = Base64Utils.encode(original);
        assertEquals(original, Base64Utils.decode(encoded));
    }

    @Test
    void encodeProducesBase64() {
        String encoded = Base64Utils.encode("test");
        // "test" in base64 is "dGVzdA=="
        assertEquals("dGVzdA==", encoded);
    }

    @Test
    void decodeKnownValue() {
        assertEquals("test", Base64Utils.decode("dGVzdA=="));
    }

    @Test
    void encodePNGAsUrlBytes() {
        byte[] fakeData = {(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes
        String result = Base64Utils.encodePNGAsUrl(fakeData);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void encodePNGAsUrlRoundTrip() {
        byte[] data = "fake png data".getBytes(StandardCharsets.UTF_8);
        String dataUri = Base64Utils.encodePNGAsUrl(data);
        String base64Part = dataUri.substring("data:image/png;base64,".length());
        assertArrayEquals(data, Base64.getDecoder().decode(base64Part));
    }
}
