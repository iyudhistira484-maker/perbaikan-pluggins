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
package net.skinsrestorer.bukkit.refresher;

import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.entity.Player;

public interface SkinRefresher {
    SkinRefresher NO_OP = new SkinRefresher() {
        @Override
        public void refresh(Player player, SkinProperty property) {
            // No-op
        }

        @Override
        public void resendInfoPackets(Player toResend, Player toSendTo) {
            // No-op
        }

        @Override
        public boolean needsManualOtherRefresh() {
            return true;
        }

        @Override
        public boolean needsManualPropertyApply() {
            return true;
        }
    };

    void refresh(Player player, SkinProperty property);

    void resendInfoPackets(Player toResend, Player toSendTo);

    boolean needsManualOtherRefresh();

    boolean needsManualPropertyApply();
}
