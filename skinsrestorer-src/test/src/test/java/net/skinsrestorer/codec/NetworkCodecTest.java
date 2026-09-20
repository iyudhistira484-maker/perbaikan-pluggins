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
package net.skinsrestorer.codec;

import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.codec.BuiltInCodecs;
import net.skinsrestorer.shared.codec.NetworkCodec;
import net.skinsrestorer.shared.codec.SRInputReader;
import net.skinsrestorer.shared.codec.SROutputWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NetworkCodecTest {
    private <T> T roundTrip(NetworkCodec<T> codec, T value) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        SROutputWriter writer = new SROutputWriter(dos);
        codec.write(writer, value);
        SRInputReader reader = new SRInputReader(baos.toByteArray());
        return codec.read(reader);
    }

    @Test
    void stringCodec() {
        assertEquals("hello", roundTrip(BuiltInCodecs.STRING_CODEC, "hello"));
        assertEquals("", roundTrip(BuiltInCodecs.STRING_CODEC, ""));
        assertEquals("unicode: \u00e9\u00e0\u00fc", roundTrip(BuiltInCodecs.STRING_CODEC, "unicode: \u00e9\u00e0\u00fc"));
    }

    @Test
    void intCodec() {
        assertEquals(0, roundTrip(BuiltInCodecs.INT_CODEC, 0));
        assertEquals(42, roundTrip(BuiltInCodecs.INT_CODEC, 42));
        assertEquals(-1, roundTrip(BuiltInCodecs.INT_CODEC, -1));
        assertEquals(Integer.MAX_VALUE, roundTrip(BuiltInCodecs.INT_CODEC, Integer.MAX_VALUE));
    }

    @Test
    void booleanCodec() {
        assertEquals(true, roundTrip(BuiltInCodecs.BOOLEAN_CODEC, true));
        assertEquals(false, roundTrip(BuiltInCodecs.BOOLEAN_CODEC, false));
    }

    @Test
    void uuidCodec() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, roundTrip(BuiltInCodecs.UUID_CODEC, uuid));
    }

    @Test
    void uuidCodecSpecificValue() {
        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        assertEquals(uuid, roundTrip(BuiltInCodecs.UUID_CODEC, uuid));
    }

    @Test
    void skinPropertyCodec() {
        SkinProperty prop = SkinProperty.of("value123", "signature456");
        SkinProperty result = roundTrip(BuiltInCodecs.SKIN_PROPERTY_CODEC, prop);
        assertEquals(prop, result);
    }

    @Test
    void skinVariantCodec() {
        assertEquals(SkinVariant.CLASSIC, roundTrip(BuiltInCodecs.SKIN_VARIANT_CODEC, SkinVariant.CLASSIC));
        assertEquals(SkinVariant.SLIM, roundTrip(BuiltInCodecs.SKIN_VARIANT_CODEC, SkinVariant.SLIM));
    }

    @Test
    void skinTypeCodec() {
        for (SkinType type : SkinType.values()) {
            assertEquals(type, roundTrip(BuiltInCodecs.SKIN_TYPE_CODEC, type));
        }
    }

    @Test
    void skinIdentifierCustom() {
        SkinIdentifier id = SkinIdentifier.ofCustom("myskin");
        SkinIdentifier result = roundTrip(BuiltInCodecs.SKIN_IDENTIFIER_CODEC, id);
        assertEquals(id, result);
    }

    @Test
    void skinIdentifierPlayer() {
        SkinIdentifier id = SkinIdentifier.ofPlayer(UUID.randomUUID());
        SkinIdentifier result = roundTrip(BuiltInCodecs.SKIN_IDENTIFIER_CODEC, id);
        assertEquals(id, result);
    }

    @Test
    void skinIdentifierUrl() {
        SkinIdentifier id = SkinIdentifier.ofURL("https://example.com/skin.png", SkinVariant.SLIM);
        SkinIdentifier result = roundTrip(BuiltInCodecs.SKIN_IDENTIFIER_CODEC, id);
        assertEquals(id, result);
    }

    @Test
    void skinIdentifierUrlNoVariant() {
        SkinIdentifier id = SkinIdentifier.ofURL("https://example.com/skin.png", null);
        SkinIdentifier result = roundTrip(BuiltInCodecs.SKIN_IDENTIFIER_CODEC, id);
        assertEquals(id, result);
    }

    @Test
    void optionalPresent() {
        NetworkCodec<Optional<String>> codec = BuiltInCodecs.STRING_CODEC.optional();
        Optional<String> value = Optional.of("present");
        assertEquals(value, roundTrip(codec, value));
    }

    @Test
    void optionalEmpty() {
        NetworkCodec<Optional<String>> codec = BuiltInCodecs.STRING_CODEC.optional();
        Optional<String> value = Optional.empty();
        assertEquals(value, roundTrip(codec, value));
    }

    @Test
    void listCodec() {
        NetworkCodec<List<String>> codec = BuiltInCodecs.STRING_CODEC.list();
        List<String> value = List.of("a", "b", "c");
        assertEquals(value, roundTrip(codec, value));
    }

    @Test
    void listCodecEmpty() {
        NetworkCodec<List<String>> codec = BuiltInCodecs.STRING_CODEC.list();
        assertEquals(List.of(), roundTrip(codec, List.of()));
    }

    @Test
    void mapCodec() {
        NetworkCodec<Map<String, Integer>> codec = BuiltInCodecs.STRING_CODEC.mappedTo(BuiltInCodecs.INT_CODEC);
        Map<String, Integer> value = new LinkedHashMap<>();
        value.put("a", 1);
        value.put("b", 2);
        assertEquals(value, roundTrip(codec, value));
    }

    @Test
    void mapCodecEmpty() {
        NetworkCodec<Map<String, Integer>> codec = BuiltInCodecs.STRING_CODEC.mappedTo(BuiltInCodecs.INT_CODEC);
        assertEquals(Map.of(), roundTrip(codec, Map.of()));
    }

    @Test
    void compressedCodec() {
        NetworkCodec<String> codec = BuiltInCodecs.STRING_CODEC.compressed();
        String value = "This is a test string that will be gzip compressed";
        assertEquals(value, roundTrip(codec, value));
    }

    @Test
    void mapTransform() {
        NetworkCodec<Long> longCodec = BuiltInCodecs.STRING_CODEC.map(
                Object::toString,
                Long::parseLong
        );
        assertEquals(12345L, roundTrip(longCodec, 12345L));
    }

    @Test
    void unitCodec() {
        String instance = "singleton";
        NetworkCodec<String> codec = NetworkCodec.unit(instance);
        assertSame(instance, roundTrip(codec, instance));
    }
}
