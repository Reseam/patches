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

import android.content.Context;

import java.lang.reflect.Field;

final class StoryOwnerResolver {
    private static final String REEL_ITEM_CLASS = "com.instagram.model.reels.ReelItem";

    private StoryOwnerResolver() {}

    static Object reelItem(Object owner) {
        if (owner == null) return null;
        for (Field field : Reflect.fields(owner.getClass())) {
            if (REEL_ITEM_CLASS.equals(field.getType().getName())) {
                return Reflect.fieldValue(field, owner);
            }
        }
        return null;
    }

    static Context context(Object owner) {
        if (owner == null) return ContextResolver.fromObjects();
        for (Field field : Reflect.fields(owner.getClass())) {
            if (Context.class.isAssignableFrom(field.getType())) {
                Object value = Reflect.fieldValue(field, owner);
                if (value instanceof Context) return ContextResolver.safe((Context) value);
            }
        }
        return ContextResolver.fromObjects(owner);
    }
}
