// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.content.Context;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class MediaDownloader {
    private static final Map<Object, Boolean> REELS_MENUS = Collections.synchronizedMap(new WeakHashMap<>());

    private MediaDownloader() {}

    public static boolean isStoryDownload(CharSequence option) {
        return StoryOptions.isDownload(option);
    }

    public static CharSequence[] appendStoryDownload(CharSequence[] items) {
        return StoryOptions.appendDownload(items);
    }

    public static boolean handleFeedMenuClick(Object media, Object option, Context context, int currentIndex) {
        return FeedClickHandler.handle(media, option, context, currentIndex);
    }

    public static void downloadMedia(Object media, Context context) {
        DownloadEnqueuer.download(media, context);
    }

    public static void downloadStory(Object owner) {
        DownloadEnqueuer.downloadStory(owner);
    }

    public static boolean claimReelsMenu(Object menu) {
        if (menu == null) return false;
        synchronized (REELS_MENUS) {
            return REELS_MENUS.put(menu, Boolean.TRUE) == null;
        }
    }

    public static boolean hasDownloadOption(List<?> options, Object option) {
        if (options == null || option == null) return false;
        for (Object item : options) {
            if (item == option) return true;
            if (item == null) continue;
            try {
                if (FeedMenuOption.option(item) == option) return true;
            } catch (ClassCastException ignored) {}
        }
        return false;
    }

    public static void labelDownloadOption(List<?> options, Object option) {
        if (options == null || option == null) return;
        for (Object row : options) {
            if (row == null) continue;
            try {
                if (FeedMenuOption.option(row) == option) FeedMenuOption.setLabel(row, "Download");
            } catch (ClassCastException ignored) {}
        }
    }

    public static List<?> withDownloadOption(List<?> options, Object option) {
        if (options == null || option == null || options.contains(option)) return options;
        java.util.ArrayList<Object> updated = new java.util.ArrayList<>(options);
        updated.add(option);
        return updated;
    }
}
