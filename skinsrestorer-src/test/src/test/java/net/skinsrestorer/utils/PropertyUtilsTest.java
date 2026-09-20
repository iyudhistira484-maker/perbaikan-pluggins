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

import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyUtilsTest {
    private static final String TEXTURE_HASH = "a".repeat(64);

    private static String encode(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String skinValue(String model) {
        String metadata = model == null ? "" : ",\"metadata\":{\"model\":\"%s\"}".formatted(model);

        return encode("""
                {"timestamp":0,"profileId":"00000000000000000000000000000000","profileName":"Test",\
                "textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/%s"%s}}}\
                """.formatted(TEXTURE_HASH, metadata));
    }

    @Test
    void skinVariantSlim() {
        assertEquals(SkinVariant.SLIM, PropertyUtils.getSkinVariant(skinValue("slim")));
    }

    @Test
    void skinVariantClassic() {
        assertEquals(SkinVariant.CLASSIC, PropertyUtils.getSkinVariant(skinValue("classic")));
        assertEquals(SkinVariant.CLASSIC, PropertyUtils.getSkinVariant(skinValue(null)));

        // A slim model written in a different case must still be recognized.
        assertEquals(SkinVariant.SLIM, PropertyUtils.getSkinVariant(skinValue("SLIM")));
    }

    /**
     * MineSkin can return incomplete data. Reading the variant of such a value used to throw a
     * NullPointerException, which aborted the whole skin lookup after the skin was generated and
     * left the player without a skin even though the command reported success.
     */
    @Test
    void skinVariantOfBrokenValues() {
        assertDoesNotThrow(() -> {
            assertEquals(SkinVariant.CLASSIC, PropertyUtils.getSkinVariant(""));
            assertEquals(SkinVariant.CLASSIC, PropertyUtils.getSkinVariant("not-base64!"));
            assertEquals(SkinVariant.CLASSIC, PropertyUtils.getSkinVariant(encode("{}")));
            assertEquals(SkinVariant.CLASSIC, PropertyUtils.getSkinVariant(encode("{\"textures\":{}}")));
            assertEquals(SkinVariant.CLASSIC, PropertyUtils.getSkinVariant(encode("{\"textures\":{\"CAPE\":{\"url\":\"x\"}}}")));
        });
    }

    @Test
    void hasSkinTexture() {
        assertTrue(PropertyUtils.hasSkinTexture(skinValue("slim")));
        assertTrue(PropertyUtils.hasSkinTexture(SkinProperty.of(skinValue(null), "signature")));

        assertFalse(PropertyUtils.hasSkinTexture(""));
        assertFalse(PropertyUtils.hasSkinTexture("not-base64!"));
        assertFalse(PropertyUtils.hasSkinTexture(encode("{}")));
        // A value that only contains a cape cannot be rendered as a skin.
        assertFalse(PropertyUtils.hasSkinTexture(encode("{\"textures\":{\"CAPE\":{\"url\":\"http://textures.minecraft.net/texture/x\"}}}")));
        assertFalse(PropertyUtils.hasSkinTexture(encode("{\"textures\":{\"SKIN\":{}}}")));
    }
}
