/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.download;

import java.lang.reflect.Field;

final class ReelItemResolver {
    private ReelItemResolver() {}

    static Object media(Object reelItem) {
        if (reelItem == null) return null;

        Object media = mediaFromFields(reelItem, true);
        if (media != null) return media;

        return mediaFromFields(reelItem, false);
    }

    private static Object mediaFromFields(Object reelItem, boolean requireFinal) {
        for (Field field : Reflect.fields(reelItem.getClass())) {
            if (field.getType().isPrimitive()) continue;
            if (requireFinal && !java.lang.reflect.Modifier.isFinal(field.getModifiers())) continue;

            Object value = Reflect.fieldValue(field, reelItem);
            if (value != null && UrlExtractor.best(value) != null) return value;
        }
        return null;
    }
}
