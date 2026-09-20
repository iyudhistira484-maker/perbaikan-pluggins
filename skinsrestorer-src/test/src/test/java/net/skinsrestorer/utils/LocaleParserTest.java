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

import net.skinsrestorer.shared.utils.LocaleParser;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class LocaleParserTest {
    @Test
    void parseLocaleLanguageOnly() {
        Locale locale = LocaleParser.parseLocale("en").orElseThrow();
        assertEquals("en", locale.getLanguage());
    }

    @Test
    void parseLocaleLanguageAndCountry() {
        Locale locale = LocaleParser.parseLocale("en_US").orElseThrow();
        assertEquals("en", locale.getLanguage());
        assertEquals("US", locale.getCountry());
    }

    @Test
    void parseLocaleNull() {
        assertTrue(LocaleParser.parseLocale(null).isEmpty());
    }

    @Test
    void parseLocaleTooManyParts() {
        assertTrue(LocaleParser.parseLocale("a_b_c").isEmpty());
    }

    @Test
    void parseLocaleStrictValid() {
        Locale locale = LocaleParser.parseLocaleStrict("de_DE");
        assertEquals("de", locale.getLanguage());
        assertEquals("DE", locale.getCountry());
    }

    @Test
    void parseLocaleStrictLanguageOnly() {
        Locale locale = LocaleParser.parseLocaleStrict("fr");
        assertEquals("fr", locale.getLanguage());
    }

    @Test
    void parseLocaleStrictInvalid() {
        assertThrows(IllegalArgumentException.class, () -> LocaleParser.parseLocaleStrict("a_b_c"));
    }
}
