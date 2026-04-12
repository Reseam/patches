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

final class ContextResolver {
    private ContextResolver() {}

    static Context best(Context explicit, Object... owners) {
        Context safe = safe(explicit);
        if (safe != null) return safe;
        return fromObjects(owners);
    }

    static Context fromObjects(Object... owners) {
        for (Object owner : owners) {
            Context context = find(owner);
            if (context != null) return safe(context);
        }
        return app();
    }

    static Context find(Object object) {
        Object context = Reflect.find(object, ContextResolver::isContext, 6);
        return context instanceof Context ? (Context) context : null;
    }

    static Context safe(Context context) {
        try {
            if (context != null && context.getApplicationContext() != null) {
                return context.getApplicationContext();
            }
        } catch (Throwable ignored) {}
        return app();
    }

    private static boolean isContext(Object object) {
        return object instanceof Context;
    }

    private static Context app() {
        try {
            Object activityThread = Class.forName("android.app.ActivityThread")
                    .getMethod("currentActivityThread")
                    .invoke(null);
            return (Context) activityThread.getClass()
                    .getMethod("getApplication")
                    .invoke(activityThread);
        } catch (Throwable t) {
            Logcat.e("getAppContext failed", t);
            return null;
        }
    }
}
