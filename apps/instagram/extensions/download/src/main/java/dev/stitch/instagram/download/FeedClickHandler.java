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

final class FeedClickHandler {
    private FeedClickHandler() {}

    static boolean handleFeedClick(Object handler, Object option) {
        Logcat.d("handleFeedClick entry");
        if (handler == null || option == null) return false;
        if (!DownloadOption.isDownload(option)) return false;

        Context context = ContextResolver.fromObjects(handler);
        try {
            Object media = MediaMeta.feedHandlerMedia(handler);
            if (media != null) {
                DownloadEnqueuer.download(media, context);
            } else {
                Ui.showToast(context, "Could not extract media URL");
            }
        } catch (Throwable t) {
            Logcat.e("handleFeedClick failed", t);
            Ui.showToast(context, "Download failed");
        }
        return true;
    }
}
