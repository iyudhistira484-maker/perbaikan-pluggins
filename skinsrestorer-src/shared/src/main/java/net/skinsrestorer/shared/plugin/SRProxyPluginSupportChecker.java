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
package net.skinsrestorer.shared.plugin;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.log.SRLogger;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRProxyPluginSupportChecker {
    private static final String VIA_VERSION_PLUGIN_ID = "ViaVersion";

    private final SRProxyAdapter adapter;
    private final SRLogger logger;

    public void checkViaVersionProxyInstall() {
        if (adapter.getPluginInfo(VIA_VERSION_PLUGIN_ID).isEmpty()) {
            return;
        }

        logger.warning("ViaVersion is installed on the proxy. You should not run ViaVersion or other Via* plugins on your proxy, because this can cause issues with skin refreshing. Install Via* plugins only on all backend servers, never on the proxy.");
    }
}
