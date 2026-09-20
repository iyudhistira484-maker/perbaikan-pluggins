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
package net.skinsrestorer;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.extern.slf4j.Slf4j;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.utils.MetricsCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith({MockitoExtension.class, SRExtension.class})
class MineSkinTest {
    private static final String TEST_URL = "https://skinsrestorer.net/skinsrestorer-skin.png";
    private static final String MINE_SKIN_UUID = "ac5a93ea-382d-4e5d-b8f3-21fe6f0e22f0";
    private static final String TEXTURE_VALUE = Base64.getEncoder().encodeToString("""
            {"timestamp":0,"profileId":"00000000000000000000000000000000","profileName":"Test","signatureRequired":true,\
            "textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/%s"}}}"""
            .formatted("a".repeat(64)).getBytes(StandardCharsets.UTF_8));
    private static final String SKIN_JSON = """
            {"success":true,"skin":{"uuid":"%s","variant":"classic","visibility":"public",\
            "texture":{"data":{"value":"%s","signature":"test-signature"}}}}"""
            .formatted(MINE_SKIN_UUID.replace("-", ""), TEXTURE_VALUE);
    @Mock
    private SettingsManager settings;
    @Mock
    private SkinsRestorerLocale skinsRestorerLocale;
    @Mock
    private HttpClient httpClient;

    @Test
    void services(Injector injector) {
        assertDoesNotThrow(() -> {
            injector.register(SkinsRestorerLocale.class, skinsRestorerLocale);

            when(settings.getProperty(APIConfig.MINESKIN_API_KEY)).thenReturn("");
            when(settings.getProperty(AdvancedConfig.NO_CONNECTIONS)).thenReturn(false);

            injector.register(SettingsManager.class, settings);

            String randomUrl = TEST_URL + "?" + UUID.randomUUID(); // Random URL to avoid caching
            MetricsCounter metricsCounter = injector.getSingleton(MetricsCounter.class);

            try {
                MineSkinResponse response = injector.getSingleton(MineSkinAPIImpl.class)
                        .genSkin(randomUrl, null);
            } catch (Exception e) {
                log.error("Failed to generate skin", e);
            }

            /*

        assertNotNull(response);

        assertEquals(1, metricsCounter.collect(MetricsCounter.Service.MINE_SKIN));
         */
        });

        /*

        assertNotNull(response);

        assertEquals(1, metricsCounter.collect(MetricsCounter.Service.MINE_SKIN));
         */
    }

    /**
     * The skin upload page hands out https://minesk.in/<uuid> links, which redirect to an HTML page.
     * MineSkin can not download them again, so they have to be resolved through the skin endpoint.
     */
    @Test
    void resolvesMineSkinShortLinks(Injector injector) throws Exception {
        injector.register(SkinsRestorerLocale.class, skinsRestorerLocale);
        injector.register(SettingsManager.class, settings);
        injector.register(HttpClient.class, httpClient);

        when(settings.getProperty(APIConfig.MINESKIN_API_KEY)).thenReturn("");
        when(httpClient.execute(any(), isNull(), any(), anyString(), any(), anyMap(), anyInt()))
                .thenReturn(new HttpResponse(200, SKIN_JSON, Map.of()));

        MineSkinResponse response = injector.getSingleton(MineSkinAPIImpl.class)
                .genSkin("https://minesk.in/" + MINE_SKIN_UUID, null);

        assertNotNull(response);
        assertEquals(SkinVariant.CLASSIC, response.getGeneratedVariant());
        assertEquals(TEXTURE_VALUE, response.getProperty().getValue());

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(httpClient).execute(uriCaptor.capture(), isNull(), eq(HttpClient.HttpType.JSON), anyString(),
                eq(HttpClient.HttpMethod.GET), anyMap(), anyInt());
        assertEquals("https://api.mineskin.org/v2/skins/" + MINE_SKIN_UUID, uriCaptor.getValue().toString());

        // The skin already exists on MineSkin, so no new generation may be requested
        verify(httpClient, never()).execute(any(), any(), any(), anyString(), eq(HttpClient.HttpMethod.POST), anyMap(), anyInt());
    }

    /**
     * Regular image urls must keep being sent to MineSkin for generation.
     */
    @Test
    void generatesSkinsForRegularImageUrls(Injector injector) throws Exception {
        injector.register(SkinsRestorerLocale.class, skinsRestorerLocale);
        injector.register(SettingsManager.class, settings);
        injector.register(HttpClient.class, httpClient);

        when(settings.getProperty(APIConfig.MINESKIN_API_KEY)).thenReturn("");
        when(settings.getProperty(APIConfig.MINESKIN_SECRET_SKINS)).thenReturn(false);
        when(httpClient.execute(any(), any(), any(), anyString(), any(), anyMap(), anyInt()))
                .thenReturn(new HttpResponse(200, SKIN_JSON, Map.of()));

        MineSkinResponse response = injector.getSingleton(MineSkinAPIImpl.class)
                .genSkin(TEST_URL, null);

        assertNotNull(response);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(httpClient).execute(uriCaptor.capture(), any(), eq(HttpClient.HttpType.JSON), anyString(),
                eq(HttpClient.HttpMethod.POST), anyMap(), anyInt());
        assertEquals("https://api.mineskin.org/v2/generate", uriCaptor.getValue().toString());

        verify(httpClient, never()).execute(any(), isNull(), any(), anyString(), eq(HttpClient.HttpMethod.GET), anyMap(), anyInt());
    }
}
