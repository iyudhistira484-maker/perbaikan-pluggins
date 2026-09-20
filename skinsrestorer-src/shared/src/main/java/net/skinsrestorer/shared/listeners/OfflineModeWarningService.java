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
package net.skinsrestorer.shared.listeners;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.LoginConfig;
import net.skinsrestorer.shared.floodgate.FloodgateUtil;
import net.skinsrestorer.shared.info.PlatformType;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.storage.PlayerStorageImpl;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;

import javax.inject.Inject;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class OfflineModeWarningService {
    private final SettingsManager settings;
    private final SRPlatformAdapter adapter;
    private final PlayerStorageImpl playerStorage;
    private final Injector injector;
    private final Set<UUID> pendingWarnings = ConcurrentHashMap.newKeySet();

    public void recordLogin(UUID uuid, boolean hasOnlineProperties) {
        if (hasOnlineProperties || FloodgateUtil.isFloodgateBedrockPlayer(uuid)) {
            pendingWarnings.remove(uuid);
            return;
        }

        pendingWarnings.add(uuid);
    }

    public void handleConnect(SRPlayer player) {
        if (!pendingWarnings.remove(player.getUniqueId())) {
            return;
        }

        if (isRunningInProxyMode()) {
            return;
        }

        if (!settings.getProperty(LoginConfig.OFFLINE_MODE_WARNING_ENABLED)) {
            return;
        }

        if (playerStorage.isOfflineModeWarningDismissed(player.getUniqueId())) {
            return;
        }

        player.sendMessage(Message.OFFLINE_MODE_SKIN_WARNING);
    }

    private boolean isRunningInProxyMode() {
        if (adapter.getPlatform().getPlatformType() == PlatformType.PROXY) {
            return true;
        }

        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
        return serverPlugin != null && serverPlugin.isProxyMode();
    }
}
