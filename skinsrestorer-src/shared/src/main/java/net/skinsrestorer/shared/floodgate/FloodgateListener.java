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
package net.skinsrestorer.shared.floodgate;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.shared.log.SRLogger;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class FloodgateListener implements Consumer<SkinApplyEvent> {
    private final SRLogger logger;
    private final PlayerStorage playerStorage;

    @Override
    public void accept(SkinApplyEvent event) {
        FloodgatePlayer floodgatePlayer = event.player();
        UUID uuid = floodgatePlayer.getCorrectUniqueId();
        String name = floodgatePlayer.getCorrectUsername();
        logger.debug("Handling Floodgate skin apply for %s (%s)".formatted(name, uuid));

        Optional<SkinProperty> optional;
        try {
            optional = playerStorage.getSkinForPlayer(uuid, name, true);
        } catch (DataRequestException e) {
            logger.warning("Failed to get skin for %s (%s)".formatted(name, uuid), e);
            return;
        }

        if (optional.isEmpty()) {
            // The player did not select a skin with SkinsRestorer, so Floodgate keeps handling it
            // (showing the skin the Bedrock client sent) and we do not touch the event.
            logger.debug("No SkinsRestorer skin stored for %s (%s), keeping the Floodgate skin.".formatted(name, uuid));
            return;
        }

        SkinProperty skinProperty = optional.get();
        if (!PropertyUtils.hasSkinTexture(skinProperty)) {
            logger.warning("The stored skin of %s (%s) does not contain a skin texture, keeping the Floodgate skin. Please set the skin again."
                    .formatted(name, uuid));
            return;
        }

        event.newSkin(new SkinDataImpl(skinProperty.getValue(), skinProperty.getSignature()));

        // Floodgate cancels this event by default for players that are linked to a Java account, as it
        // only applies skins to players that don't have one yet. Without un-cancelling, the skin above
        // would silently be ignored and the player would keep showing their Bedrock/default skin.
        event.setCancelled(false);
    }

    private record SkinDataImpl(String value, String signature) implements SkinApplyEvent.SkinData {
    }
}
