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

public final class MediaDownloader {
    private MediaDownloader() {}

    public static boolean isDownloadOption(Object option) {
        return DownloadOption.isDownload(option);
    }

    public static boolean isStoryDownload(CharSequence option) {
        return StoryOptions.isDownload(option);
    }

    public static CharSequence[] appendStoryDownload(CharSequence[] items) {
        return StoryOptions.appendDownload(items);
    }

    public static boolean handleFeedClick(Object handler, Object option) {
        return FeedClickHandler.handleFeedClick(handler, option);
    }

    public static void addLegacyDownloadRow(Object menu, Object media, Context context) {
        MenuInjector.addLegacyRow(menu, media, context);
    }

    public static void downloadMedia(Object media, Context context) {
        DownloadEnqueuer.download(media, context);
    }

    public static void downloadReelItem(Object reelItem, Context context) {
        DownloadEnqueuer.downloadReelItem(reelItem, context);
    }

    public static void downloadStory(Object owner) {
        DownloadEnqueuer.downloadStory(owner);
    }
}
