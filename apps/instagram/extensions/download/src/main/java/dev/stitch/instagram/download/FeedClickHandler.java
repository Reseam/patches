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
import java.lang.reflect.Modifier;

final class FeedClickHandler {
    private FeedClickHandler() {}

    static boolean handleListener(Object listener) {
        if (listener == null) return false;

        Object option = findDownloadOption(listener);
        if (!DownloadOption.isDownload(option)) return false;

        Context context = ContextResolver.fromObjects(listener);
        try {
            Object media = mediaFromListener(listener);
            Logcat.d("handleFeedMenuClick: listener=" + Reflect.className(listener)
                    + ", media=" + Reflect.className(media)
                    + ", context=" + Reflect.className(context));

            if (media != null) {
                DownloadEnqueuer.download(media, context);
            } else {
                Ui.showToast(context, "Could not extract media URL");
            }
        } catch (Throwable t) {
            Logcat.e("handleFeedMenuClick failed", t);
            Ui.showToast(context, "Download failed");
        }
        return true;
    }

    private static Object findDownloadOption(Object object) {
        if (object == null) return null;
        if (DownloadOption.isDownload(object)) return object;

        for (Field field : Reflect.fields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            Object value = Reflect.fieldValue(field, object);
            if (DownloadOption.isDownload(value)) return value;
            Object nested = findDirectDownloadOption(value);
            if (nested != null) return nested;
        }
        return null;
    }

    private static Object findDirectDownloadOption(Object object) {
        if (object == null) return null;
        if (DownloadOption.isDownload(object)) return object;
        if (!isInspectable(object)) return null;

        for (Field field : Reflect.fields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            Object value = Reflect.fieldValue(field, object);
            if (DownloadOption.isDownload(value)) return value;
        }
        return null;
    }

    private static Object mediaFromListener(Object listener) {
        for (Field field : Reflect.fields(listener.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            Object value = Reflect.fieldValue(field, listener);
            Object media = mediaFromOwner(value);
            if (media != null) return media;
        }
        return null;
    }

    private static Object mediaFromOwner(Object owner) {
        if (owner == null) return null;

        for (Field field : Reflect.fields(owner.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            Object value = Reflect.fieldValue(field, owner);
            if (isMediaLike(value)) return value;
        }
        return null;
    }

    private static boolean isMediaLike(Object value) {
        if (value == null) return false;
        if (!isInspectable(value)) return false;

        try {
            return UrlExtractor.best(value) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isInspectable(Object value) {
        Class<?> cls = value.getClass();
        if (cls.isPrimitive() || cls.isArray() || cls.isEnum()) return false;
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) return false;
        if (value instanceof Iterable<?>) return false;

        String name = cls.getName();
        if (name.startsWith("java.") || name.startsWith("android.") || name.startsWith("androidx.")) return false;
        return true;
    }
}
