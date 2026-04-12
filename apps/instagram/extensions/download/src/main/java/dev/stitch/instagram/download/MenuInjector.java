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
import android.view.View;

import java.lang.reflect.Method;

final class MenuInjector {
    private MenuInjector() {}

    static void addActionSheetRow(Object menu, Object media, Context context) {
        if (menu == null || media == null) return;
        if (StoryOptions.containsDownload(menu)) return;

        Context rowContext = ContextResolver.best(context, menu);
        Method add = findActionSheetRowMethod(menu.getClass());
        if (rowContext == null || add == null) return;

        Reflect.invoke(add, menu, "Download", listener(media, rowContext));
        Logcat.d("addDownloadRow: added to " + Reflect.className(menu));
    }

    static void addLegacyRow(Object menu, Object media, Context context) {
        if (menu == null || media == null) return;
        if (StoryOptions.containsDownload(menu)) return;

        Context rowContext = ContextResolver.best(context, menu);
        Method add = findLegacyRowMethod(menu.getClass());
        if (rowContext == null || add == null) return;

        Reflect.invoke(add, menu, rowContext, listener(media, rowContext), "Download", 0, false);
        Logcat.d("addLegacyDownloadRow: added to " + Reflect.className(menu));
    }

    private static Method findActionSheetRowMethod(Class<?> cls) {
        for (Method method : Reflect.methods(cls)) {
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 2 && params[0] == String.class
                    && View.OnClickListener.class.isAssignableFrom(params[1])) {
                return method;
            }
        }
        return null;
    }

    private static Method findLegacyRowMethod(Class<?> cls) {
        for (Method method : Reflect.methods(cls)) {
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 5
                    && Context.class.isAssignableFrom(params[0])
                    && View.OnClickListener.class.isAssignableFrom(params[1])
                    && params[2] == String.class
                    && params[3] == int.class
                    && params[4] == boolean.class) {
                return method;
            }
        }
        return null;
    }

    private static View.OnClickListener listener(Object media, Context context) {
        return view -> DownloadEnqueuer.download(media, context != null ? context : view.getContext());
    }
}
