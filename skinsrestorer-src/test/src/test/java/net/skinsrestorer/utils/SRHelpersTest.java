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

import net.skinsrestorer.shared.utils.SRHelpers;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SRHelpersTest {
    @Test
    void hashSHA256ToBytesIsDeterministic() {
        byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(SRHelpers.hashSHA256ToBytes(input), SRHelpers.hashSHA256ToBytes(input));
    }

    @Test
    void hashSHA256ToBytesLength() {
        byte[] hash = SRHelpers.hashSHA256ToBytes("test".getBytes(StandardCharsets.UTF_8));
        assertEquals(32, hash.length); // SHA-256 is always 32 bytes
    }

    @Test
    void hashSha256ToHexKnownValue() {
        // SHA-256 of "hello" is well-known
        String hex = SRHelpers.hashSha256ToHex("hello");
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hex);
    }

    @Test
    void hashSha256ToHexDifferentInputs() {
        assertNotEquals(SRHelpers.hashSha256ToHex("hello"), SRHelpers.hashSha256ToHex("world"));
    }

    @Test
    void hashSha256ToLongIsDeterministic() {
        assertEquals(SRHelpers.hashSha256ToLong("test"), SRHelpers.hashSha256ToLong("test"));
    }

    @Test
    void hashSha256ToLongDifferentInputs() {
        assertNotEquals(SRHelpers.hashSha256ToLong("hello"), SRHelpers.hashSha256ToLong("world"));
    }

    @Test
    void parseURLValid() {
        Optional<URL> url = SRHelpers.parseURL("https://example.com/path");
        assertTrue(url.isPresent());
        assertEquals("example.com", url.get().getHost());
        assertEquals("https", url.get().getProtocol());
    }

    @Test
    void parseURLInvalid() {
        assertTrue(SRHelpers.parseURL("not a url").isEmpty());
        assertTrue(SRHelpers.parseURL("").isEmpty());
    }

    @Test
    void sanitizeImageURLNamemcSkin() {
        String result = SRHelpers.sanitizeImageURL("https://namemc.com/skin/abc123");
        assertEquals("https://s.namemc.com/i/abc123.png", result);
    }

    @Test
    void sanitizeImageURLNamemcSubdomain() {
        String result = SRHelpers.sanitizeImageURL("https://www.namemc.com/skin/abc123");
        assertEquals("https://s.namemc.com/i/abc123.png", result);
    }

    @Test
    void sanitizeImageURLNonNamemc() {
        String url = "https://example.com/skin.png";
        assertEquals(url, SRHelpers.sanitizeImageURL(url));
    }

    @Test
    void sanitizeImageURLInvalid() {
        assertEquals("not-a-url", SRHelpers.sanitizeImageURL("not-a-url"));
    }

    @Test
    void sanitizeSkinInputNamemcProfile() {
        // NameMC profile URLs with a plain username are returned as-is
        // because the extracted username part is not itself a valid skin URL
        String url = "https://namemc.com/profile/Pistonmaster.1";
        assertEquals(url, SRHelpers.sanitizeSkinInput(url));
    }

    @Test
    void sanitizeSkinInputNonNamemc() {
        String url = "https://example.com/skin.png";
        assertEquals(url, SRHelpers.sanitizeSkinInput(url));
    }

    @Test
    void sanitizeSkinInputPlainName() {
        assertEquals("Pistonmaster", SRHelpers.sanitizeSkinInput("Pistonmaster"));
    }

    @Test
    void getRootCause() {
        Exception root = new Exception("root");
        Exception wrapped = new Exception("wrapped", new Exception("middle", root));
        assertSame(root, SRHelpers.getRootCause(wrapped));
    }

    @Test
    void getRootCauseNoNesting() {
        Exception e = new Exception("solo");
        assertSame(e, SRHelpers.getRootCause(e));
    }

    @Test
    void encodeHashToTexturesValue() {
        String result = SRHelpers.encodeHashToTexturesValue("abc123");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Should be valid base64 that decodes to JSON containing the hash
        String decoded = new String(Base64.getDecoder().decode(result), StandardCharsets.UTF_8);
        assertTrue(decoded.contains("abc123"));
        assertTrue(decoded.contains("textures.minecraft.net/texture/"));
    }
}
