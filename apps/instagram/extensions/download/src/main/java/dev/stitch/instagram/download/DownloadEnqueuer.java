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

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.stitch.runtime.settings.StitchSettings;

final class DownloadEnqueuer {
    private static final String DEFAULT_DOWNLOAD_DIR = "StitchDownloads/Instagram";
    private static final String DOWNLOAD_FOLDER_KEY = "download.folder";
    private static final String SHOW_TOAST_KEY = "download.show_toast";

    private DownloadEnqueuer() {}

    static void download(Object media, Context context) {
        Logcat.d("downloadMedia: media=" + Reflect.className(media));
        if (media == null) return;

        Context safe = ContextResolver.safe(context);
        if (safe == null) {
            Logcat.e("downloadMedia: no context available");
            return;
        }

        try {
            List<?> children = UrlExtractor.carouselChildren(media);
            if (children != null && children.size() > 1) {
                downloadCarousel(children, safe);
                return;
            }
            downloadSingle(media, safe, 0);
        } catch (Throwable t) {
            Logcat.e("downloadMedia failed", t);
            showToast(safe, "Download failed");
        }
    }

    static void downloadReelItem(Object reelItem, Context context) {
        Object media = ReelItemResolver.media(reelItem);
        Logcat.d("downloadReelItem: reelItem=" + Reflect.className(reelItem)
                + ", media=" + Reflect.className(media));
        if (media != null) {
            download(media, context);
        } else {
            showToast(context, "Could not extract media URL");
        }
    }

    static void downloadStory(Object owner) {
        Object reelItem = StoryOwnerResolver.reelItem(owner);
        Context context = StoryOwnerResolver.context(owner);
        Logcat.d("downloadStory: owner=" + Reflect.className(owner)
                + ", reelItem=" + Reflect.className(reelItem));
        if (reelItem != null) {
            downloadReelItem(reelItem, context);
        } else {
            showToast(context, "Could not extract story media");
        }
    }

    private static void downloadCarousel(List<?> children, Context context) {
        int count = 0;
        for (int i = 0; i < children.size(); i++) {
            if (downloadSingle(children.get(i), context, i + 1)) count++;
        }

        if (count > 0) {
            showToast(context, "Downloading " + count + " items");
        } else {
            showToast(context, "Could not extract carousel URLs");
        }
    }

    private static boolean downloadSingle(Object media, Context context, int index) {
        if (media == null) return false;

        UrlExtractor.MediaUrl mediaUrl = UrlExtractor.best(media);
        if (mediaUrl == null) {
            if (index == 0) showToast(context, "Could not extract media URL");
            Logcat.w("Could not extract URL from " + Reflect.className(media));
            return false;
        }

        enqueue(context, mediaUrl.url, mediaUrl.video, index);
        return true;
    }

    private static void enqueue(Context context, String url, boolean isVideo, int index) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String filename = "IG_" + timestamp + (index > 0 ? "_" + index : "") + (isVideo ? ".mp4" : ".jpg");

        Logcat.d("Downloading: " + filename + " from " + url.substring(0, Math.min(80, url.length())) + "...");

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(filename);
        request.setDescription("Downloading Instagram media");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, downloadDir() + "/" + filename);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return;

        manager.enqueue(request);
        if (index <= 1) showToast(context, "Downloading " + filename);
    }

    private static String downloadDir() {
        String value = StitchSettings.getString(DOWNLOAD_FOLDER_KEY, DEFAULT_DOWNLOAD_DIR);
        if (value == null) return DEFAULT_DOWNLOAD_DIR;
        value = value.trim();
        return value.isEmpty() ? DEFAULT_DOWNLOAD_DIR : value;
    }

    private static void showToast(Context context, String message) {
        if (StitchSettings.getBoolean(SHOW_TOAST_KEY, true)) {
            Ui.showToast(context, message);
        }
    }
}
