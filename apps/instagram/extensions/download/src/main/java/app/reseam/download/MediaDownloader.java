// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

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

    public static boolean handleFeedMenuClick(Object listener) {
        return FeedClickHandler.handleListener(listener);
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
