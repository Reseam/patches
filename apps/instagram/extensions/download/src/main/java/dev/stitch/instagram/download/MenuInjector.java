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

final class MenuInjector {
    private MenuInjector() {}

    static void addLegacyRow(Object menu, Object media, Context context) {
        if (menu == null || media == null) return;

        Context rowContext = ContextResolver.best(context, menu);
        if (rowContext == null) return;

        final Context boundContext = rowContext;
        View.OnClickListener listener = view ->
                DownloadEnqueuer.download(media, boundContext != null ? boundContext : view.getContext());

        MediaMeta.addLegacyMenuRow(menu, rowContext, listener, "Download", 0, false);
    }
}
