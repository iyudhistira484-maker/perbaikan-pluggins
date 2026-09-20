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
package net.skinsrestorer.shared.connections;

import ch.jalu.configme.SettingsManager;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.mineskin.MineSkinVariant;
import net.skinsrestorer.shared.connections.mineskin.MineSkinVisibility;
import net.skinsrestorer.shared.connections.mineskin.requests.MineSkinUrlRequest;
import net.skinsrestorer.shared.connections.mineskin.responses.MineSkinUrlResponse;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.exception.MineSkinExceptionShared;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static net.skinsrestorer.shared.utils.ValidationUtil.AXOLOTL_PREFIX;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MineSkinAPIImpl implements MineSkinAPI {
    private static final int MAX_RETRIES = 5;
    private static final String MINESKIN_USER_AGENT = "SkinsRestorer/MineSkinAPI";
    private static final URI MINESKIN_ENDPOINT = URI.create("https://api.mineskin.org/v2/generate");
    private static final URI MINESKIN_SKIN_ENDPOINT = URI.create("https://api.mineskin.org/v2/skins/");
    private static final Set<String> MINESKIN_SHORT_LINK_HOSTS = Set.of("minesk.in", "mineskin.org", "www.mineskin.org");
    private static final URI AXOLOTL_DECRYPT_ENDPOINT = URI.create("https://axolotl.skinsrestorer.net/mineskin/decrypt-url");
    private final Semaphore semaphore = new Semaphore(5);
    private final Gson gson = new Gson();
    private final SRLogger logger;
    private final MetricsCounter metricsCounter;
    private final SettingsManager settings;
    private final HttpClient httpClient;
    private final AtomicLong nextRequestAt = new AtomicLong();

    @Override
    public MineSkinResponse genSkin(String imageUrl, @Nullable SkinVariant skinVariant) throws DataRequestException, MineSkinException {
        imageUrl = decryptAxolotlUrl(imageUrl);
        imageUrl = SRHelpers.sanitizeImageURL(imageUrl);

        // The skin upload page (https://skinsrestorer.net/upload) hands out MineSkin short links that
        // redirect to an HTML page instead of to an image, so MineSkin cannot download them again
        // ("invalid_image_url"). The skin behind such a link already exists, so resolve it directly
        // instead of asking MineSkin to generate it a second time.
        Optional<MineSkinResponse> existingSkin = resolveMineSkinShortLink(imageUrl, skinVariant);
        if (existingSkin.isPresent()) {
            return existingSkin.get();
        }

        try {
            int retryAttempts = 0;
            do {
                semaphore.acquire();
                try {
                    long waitDuration = nextRequestAt.get() - System.currentTimeMillis();
                    if (waitDuration > 0) {
                        logger.debug("[INFO] Waiting %dms before next MineSkin request...".formatted(waitDuration));
                        Thread.sleep(waitDuration);
                    }

                    Optional<MineSkinResponse> optional = genSkinInternal(imageUrl, skinVariant);

                    if (optional.isPresent()) {
                        return optional.get();
                    }
                } catch (IOException e) {
                    logger.debug(SRLogLevel.WARNING, "[ERROR] MineSkin Failed! IOException (connection/disk): (%s)".formatted(imageUrl), e);
                    throw new DataRequestExceptionShared(e);
                } finally {
                    semaphore.release();
                }
            } while (++retryAttempts < MAX_RETRIES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRequestExceptionShared(e);
        } catch (RuntimeException e) {
            // An unexpected response (for example invalid json) should be reported as a normal
            // request failure instead of breaking the command with an internal error.
            logger.warning("[ERROR] MineSkin Failed! Unexpected error (%s)".formatted(imageUrl), e);
            throw new DataRequestExceptionShared(e);
        }

        throw new MineSkinExceptionShared(Message.ERROR_MS_API_FAILED);
    }

    private Optional<MineSkinResponse> genSkinInternal(String imageUrl, @Nullable SkinVariant skinVariant) throws DataRequestException, MineSkinException, IOException {
        HttpResponse httpResponse = queryURL(imageUrl, skinVariant);
        logger.debug("MineSkinAPI: Response: %s".formatted(httpResponse));

        MineSkinUrlResponse response = httpResponse.getBodyAs(MineSkinUrlResponse.class);
        if (response == null) {
            logger.debug(SRLogLevel.WARNING, "[ERROR] MineSkin Failed! Empty response (Image URL: %s)".formatted(imageUrl));
            throw new MineSkinExceptionShared(Message.ERROR_MS_API_FAILED);
        }

        MineSkinUrlResponse.RateLimit rateLimit = response.getRateLimit();
        if (rateLimit != null && rateLimit.getNext() != null) {
            long serverNextRequestAt = System.currentTimeMillis() + rateLimit.getNext().getRelative();
            nextRequestAt.updateAndGet(currentValue -> Math.max(currentValue, serverNextRequestAt));
        }

        if (response.isSuccess()) {
            return Optional.of(toMineSkinResponse(response.getSkin(), skinVariant, imageUrl));
        } else {
            for (MineSkinUrlResponse.Error error : response.getErrors()) {
                logger.debug("[ERROR] MineSkin Failed! Reason: %s Image URL: %s".formatted(error, imageUrl));
                return switch (error.getCode()) {
                    case "rate_limit" -> // try again
                            Optional.empty();
                    case "failed_to_create_id", "skin_change_failed" -> {
                        logger.debug("Trying again in 6 seconds...");
                        long nowPlus = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(6);
                        nextRequestAt.updateAndGet(currentValue -> Math.max(currentValue, nowPlus));

                        yield Optional.empty(); // try again
                    }
                    case "no_account_available" -> throw new MineSkinExceptionShared(Message.ERROR_MS_FULL);
                    case "invalid_image_url" -> throw new MineSkinExceptionShared(Message.ERROR_GENERIC,
                            Placeholder.unparsed("message", "MineSkin could not download an image from \"%s\" (%s). Make sure the url points directly to a .png file."
                                    .formatted(imageUrl, error.getMessage() == null ? error.getCode() : error.getMessage())));
                    case "invalid_api_key" -> {
                        logger.severe("[ERROR] MineSkin API key is invalid! Reason: %s".formatted(error));
                        switch (error.getMessage()) {
                            case "Invalid API Key" ->
                                    logger.severe("The API Key provided is not registered on MineSkin! Please empty \"%s\" in plugins/SkinsRestorer/config.yml and run /sr reload".formatted(APIConfig.MINESKIN_API_KEY.getPath()));
                            case "Client not allowed" ->
                                    logger.severe("This server ip is not on the api key allowed IPs list!");
                            case "Origin not allowed" ->
                                    logger.severe("This server Origin is not on the api key allowed Origins list!");
                            case "Agent not allowed" ->
                                    logger.severe("SkinsRestorer's agent \"%s\" is not on the api key allowed agents list!".formatted(MINESKIN_USER_AGENT));
                            default -> logger.severe("Unknown error, please report this to SkinsRestorer's Discord!");
                        }

                        throw new MineSkinExceptionShared(Message.ERROR_MS_API_KEY_INVALID);
                    }
                    default -> throw new MineSkinExceptionShared(Message.ERROR_INVALID_URLSKIN);
                };
            }

            logger.debug("[ERROR] MineSkin Failed! Unknown error: (Image URL: %s) %d".formatted(imageUrl, httpResponse.statusCode()));
            throw new MineSkinExceptionShared(Message.ERROR_MS_API_FAILED);
        }
    }

    /**
     * Resolves a MineSkin short link (as handed out by the skin upload page) to the skin data that is
     * already stored on MineSkin, without consuming a new generation.
     *
     * @return the resolved skin, or empty when the url is not a MineSkin short link or could not be
     * resolved (the caller then falls back to a normal generation request).
     */
    private Optional<MineSkinResponse> resolveMineSkinShortLink(String imageUrl, @Nullable SkinVariant skinVariant) throws DataRequestException {
        Optional<String> skinId = extractMineSkinSkinId(imageUrl);
        if (skinId.isEmpty()) {
            return Optional.empty();
        }

        try {
            metricsCounter.increment(MetricsCounter.Service.MINESKIN_CALLS);

            Map<String, String> headers = new HashMap<>();
            getApiKey(settings).ifPresent(s ->
                    headers.put("Authorization", "Bearer %s".formatted(s)));

            HttpResponse httpResponse = httpClient.execute(
                    URI.create(MINESKIN_SKIN_ENDPOINT + skinId.get()),
                    null,
                    HttpClient.HttpType.JSON,
                    MINESKIN_USER_AGENT,
                    HttpClient.HttpMethod.GET,
                    headers,
                    30_000
            );

            if (httpResponse.statusCode() != 200) {
                logger.debug(SRLogLevel.WARNING, "Could not resolve MineSkin short link %s (HTTP %d), falling back to generating a new skin.".formatted(imageUrl, httpResponse.statusCode()));
                return Optional.empty();
            }

            MineSkinUrlResponse response = httpResponse.getBodyAs(MineSkinUrlResponse.class);
            if (response == null || !response.isSuccess()) {
                logger.debug(SRLogLevel.WARNING, "Could not resolve MineSkin short link %s, falling back to generating a new skin.".formatted(imageUrl));
                return Optional.empty();
            }

            MineSkinUrlResponse.Skin skin = response.getSkin();
            if (skin == null) {
                logger.debug(SRLogLevel.WARNING, "MineSkin short link %s did not contain any skin data, falling back to generating a new skin.".formatted(imageUrl));
                return Optional.empty();
            }

            MineSkinResponse resolved;
            try {
                resolved = toMineSkinResponse(skin, skinVariant, imageUrl);
            } catch (MineSkinException e) {
                // The link resolved, but the skin behind it is not usable. Let the normal generation
                // request try again before giving up.
                logger.debug(SRLogLevel.WARNING, "MineSkin short link %s did not contain a usable skin, falling back to generating a new skin.".formatted(imageUrl));
                return Optional.empty();
            }

            logger.debug("Resolved MineSkin short link %s to skin %s without generating a new skin.".formatted(imageUrl, skin.getUuid()));

            if (skinVariant != null && resolved.getGeneratedVariant() != null && skinVariant != resolved.getGeneratedVariant()) {
                // The skin behind the link was generated as another variant, and we cannot regenerate
                // it without the original image. Tell the user why the model is not what they asked for.
                logger.debug(SRLogLevel.WARNING, "The skin behind %s is a %s skin, so the requested %s variant is ignored."
                        .formatted(imageUrl, resolved.getGeneratedVariant(), skinVariant));
            }

            return Optional.of(resolved);
        } catch (IOException e) {
            logger.debug(SRLogLevel.WARNING, "Failed to resolve MineSkin short link %s, falling back to generating a new skin.".formatted(imageUrl), e);
            return Optional.empty();
        }
    }

    /**
     * Converts a MineSkin skin into a response, making sure the returned property can actually be
     * rendered by a Minecraft client.
     * <p>
     * A skin without a texture value or signature would be stored and applied anyway, which leaves
     * the player with their old/default skin while the command still reports success. Failing here
     * turns that silent breakage into a clear error message instead.
     */
    private MineSkinResponse toMineSkinResponse(MineSkinUrlResponse.Skin skin, @Nullable SkinVariant skinVariant, String imageUrl) throws MineSkinException {
        MineSkinUrlResponse.Skin.Texture texture = skin == null ? null : skin.getTexture();
        MineSkinUrlResponse.Skin.Texture.Data textureData = texture == null ? null : texture.getData();
        String value = textureData == null ? null : textureData.getValue();
        String signature = textureData == null ? null : textureData.getSignature();

        if (value == null || value.isEmpty() || signature == null || signature.isEmpty()) {
            logger.debug(SRLogLevel.WARNING, "MineSkin returned an incomplete skin for %s (value present: %s, signature present: %s)"
                    .formatted(imageUrl, value != null, signature != null));
            throw new MineSkinExceptionShared(Message.ERROR_MS_API_FAILED);
        }

        if (!PropertyUtils.hasSkinTexture(value)) {
            logger.debug(SRLogLevel.WARNING, "MineSkin returned a skin without a readable texture for %s".formatted(imageUrl));
            throw new MineSkinExceptionShared(Message.ERROR_MS_API_FAILED);
        }

        SkinProperty property = SkinProperty.of(value, signature);
        return MineSkinResponse.of(property, skin == null || skin.getUuid() == null ? "" : skin.getUuid(),
                skinVariant, PropertyUtils.getSkinVariant(property));
    }

    /**
     * Extracts the skin id from a MineSkin short link like {@code https://minesk.in/<uuid>}.
     */
    private Optional<String> extractMineSkinSkinId(String imageUrl) {
        Optional<URL> urlOptional = SRHelpers.parseURL(imageUrl);
        if (urlOptional.isEmpty()) {
            return Optional.empty();
        }

        URL url = urlOptional.get();
        String host = url.getHost();
        if (host == null || !MINESKIN_SHORT_LINK_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }

        String path = url.getPath();
        if (path == null) {
            return Optional.empty();
        }

        String skinId = path.startsWith("/") ? path.substring(1) : path;
        int nextSlash = skinId.indexOf('/');
        if (nextSlash != -1) {
            skinId = skinId.substring(0, nextSlash);
        }

        return skinId.isEmpty() || !skinId.matches("[A-Za-z0-9-]+") ? Optional.empty() : Optional.of(skinId);
    }

    private HttpResponse queryURL(String url, @Nullable SkinVariant skinVariant) throws IOException {
        for (int i = 0; true; i++) { // try 3 times if server not responding
            try {
                metricsCounter.increment(MetricsCounter.Service.MINESKIN_CALLS);

                Map<String, String> headers = new HashMap<>();
                getApiKey(settings).ifPresent(s ->
                        headers.put("Authorization", "Bearer %s".formatted(s)));

                return httpClient.execute(
                        MINESKIN_ENDPOINT,
                        new HttpClient.RequestBody(gson.toJson(new MineSkinUrlRequest(
                                skinVariant == null ? MineSkinVariant.UNKNOWN : switch (skinVariant) {
                                    case CLASSIC -> MineSkinVariant.CLASSIC;
                                    case SLIM -> MineSkinVariant.SLIM;
                                },
                                null,
                                settings.getProperty(APIConfig.MINESKIN_SECRET_SKINS)
                                        ? MineSkinVisibility.UNLISTED : MineSkinVisibility.PUBLIC,
                                null,
                                url
                        )), HttpClient.HttpType.JSON),
                        HttpClient.HttpType.JSON,
                        MINESKIN_USER_AGENT,
                        HttpClient.HttpMethod.POST,
                        headers,
                        90_000
                );
            } catch (IOException e) {
                if (i >= 2) {
                    throw new IOException(e);
                }
            }
        }
    }

    private Optional<String> getApiKey(SettingsManager settings) {
        String apiKey = settings.getProperty(APIConfig.MINESKIN_API_KEY);
        if (apiKey.isEmpty() || "key".equals(apiKey)) {
            return Optional.empty();
        }

        return Optional.of(apiKey);
    }

    private String decryptAxolotlUrl(String imageUrl) throws DataRequestException {
        if (!imageUrl.startsWith(AXOLOTL_PREFIX)) {
            return imageUrl;
        }

        try {
            String encodedUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
            URI requestUri = URI.create(AXOLOTL_DECRYPT_ENDPOINT + "?encryptedUrl=" + encodedUrl);

            logger.debug("Decrypting axolotl URL: %s".formatted(imageUrl));

            HttpResponse response = httpClient.execute(
                    requestUri,
                    null,
                    HttpClient.HttpType.JSON,
                    MINESKIN_USER_AGENT,
                    HttpClient.HttpMethod.GET,
                    Map.of(),
                    30_000
            );

            if (response.statusCode() == 200) {
                AxolotlDecryptResponse decryptResponse = gson.fromJson(response.body(), AxolotlDecryptResponse.class);
                if (decryptResponse.url != null && !decryptResponse.url.isEmpty()) {
                    logger.debug("Successfully decrypted axolotl URL to: %s".formatted(decryptResponse.url));
                    return decryptResponse.url;
                } else {
                    logger.debug(SRLogLevel.WARNING, "Axolotl decrypt response missing URL field");
                    throw new DataRequestExceptionShared(new IOException("Invalid decrypt response: missing URL"));
                }
            } else if (response.statusCode() == 400) {
                AxolotlErrorResponse errorResponse = gson.fromJson(response.body(), AxolotlErrorResponse.class);
                String errorMsg = errorResponse.error != null ? errorResponse.error : "Malformed ciphertext";
                logger.debug(SRLogLevel.WARNING, "Axolotl decrypt failed (400): %s".formatted(errorMsg));
                throw new DataRequestExceptionShared(new IOException("Failed to decrypt axolotl URL: " + errorMsg));
            } else if (response.statusCode() == 500) {
                AxolotlErrorResponse errorResponse = gson.fromJson(response.body(), AxolotlErrorResponse.class);
                String errorMsg = errorResponse.error != null ? errorResponse.error : "Server configuration error";
                logger.debug(SRLogLevel.WARNING, "Axolotl decrypt server error (500): %s".formatted(errorMsg));
                throw new DataRequestExceptionShared(new IOException("Axolotl decrypt service error: " + errorMsg));
            } else {
                logger.debug(SRLogLevel.WARNING, "Axolotl decrypt unexpected status: %d".formatted(response.statusCode()));
                throw new DataRequestExceptionShared(new IOException("Unexpected decrypt response: " + response.statusCode()));
            }
        } catch (IOException e) {
            logger.debug(SRLogLevel.WARNING, "Failed to decrypt axolotl URL", e);
            throw new DataRequestExceptionShared(e);
        }
    }

    private static class AxolotlDecryptResponse {
        private String url;
    }

    private static class AxolotlErrorResponse {
        private String error;
    }
}
