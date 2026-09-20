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
package net.skinsrestorer.api.connections.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class MineSkinResponse {
    private final SkinProperty property;
    private final String mineSkinId;
    private final SkinVariant requestedVariant;
    private final SkinVariant generatedVariant;

    /**
     * The variant that should be used to store and look up this skin.
     * <p>
     * Never null: it falls back to the requested variant and finally to {@link SkinVariant#CLASSIC}.
     * Storing a url skin without a variant used to result in unreadable storage entries, because
     * the variant is part of the key that the skin is saved under.
     *
     * @return the variant to store this skin under
     */
    public SkinVariant resolveVariant() {
        if (generatedVariant != null) {
            return generatedVariant;
        }

        return requestedVariant != null ? requestedVariant : SkinVariant.CLASSIC;
    }
}
