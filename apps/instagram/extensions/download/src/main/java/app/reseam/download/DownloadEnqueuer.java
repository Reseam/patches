// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import app.reseam.runtime.settings.ReseamSettings;

final class DownloadEnqueuer {
    private static final String DEFAULT_DOWNLOAD_DIR = "ReseamInsta";
    private static final String DOWNLOAD_FOLDER_KEY = "download.folder";
    private static final String SHOW_TOAST_KEY = "download.show_toast";

    private DownloadEnqueuer() {}

    static void download(Object media, Context context) {
        Logcat.d("downloadMedia: media=" + (media == null ? "null" : media.getClass().getName()));
        if (media == null) return;

        Context safe = ContextResolver.safe(context);
        if (safe == null) {
            Logcat.e("downloadMedia: no context available");
            return;
        }

        try {
            List<?> children = UrlExtractor.carouselChildren(media);
            if (children != null && children.size() > 1) {
                downloadCarousel(media, children, safe);
                return;
            }
            downloadSingle(media, media, safe, 0);
        } catch (Throwable t) {
            Logcat.e("downloadMedia failed", t);
            showToast(safe, "Download failed");
        }
    }

    static void downloadReelItem(Object reelItem, Context context) {
        Object media = ReelItemResolver.media(reelItem);
        Logcat.d("downloadReelItem: reelItem=" + (reelItem == null ? "null" : reelItem.getClass().getName())
                + ", media=" + (media == null ? "null" : media.getClass().getName()));
        if (media != null) {
            download(media, context);
        } else {
            showToast(context, "Could not extract media URL");
        }
    }

    static void downloadStory(Object owner) {
        Object reelItem = StoryOwnerResolver.reelItem(owner);
        Context context = StoryOwnerResolver.context(owner);
        Logcat.d("downloadStory: owner=" + (owner == null ? "null" : owner.getClass().getName())
                + ", reelItem=" + (reelItem == null ? "null" : reelItem.getClass().getName()));
        if (reelItem != null) {
            downloadReelItem(reelItem, context);
        } else {
            showToast(context, "Could not extract story media");
        }
    }

    private static void downloadCarousel(Object parentMedia, List<?> children, Context context) {
        int count = 0;
        for (int i = 0; i < children.size(); i++) {
            if (downloadSingle(parentMedia, children.get(i), context, i + 1)) count++;
        }

        if (count > 0) {
            showToast(context, "Downloading " + count + " items");
        } else {
            showToast(context, "Could not extract carousel URLs");
        }
    }

    private static boolean downloadSingle(Object metaMedia, Object media, Context context, int index) {
        if (media == null) return false;

        UrlExtractor.MediaUrl mediaUrl = UrlExtractor.best(media);
        if (mediaUrl == null) {
            if (index == 0) showToast(context, "Could not extract media URL");
            Logcat.w("Could not extract URL from " + (media == null ? "null" : media.getClass().getName()));
            return false;
        }

        enqueue(context, metaMedia, mediaUrl.url, mediaUrl.video, index);
        return true;
    }

    private static String buildFilename(Object media, boolean isVideo, int index) {
        Object principal = app.reseam.instagram.refs.User.fromMedia(media);
        String user = principal == null ? null : app.reseam.instagram.refs.User.username(principal);
        if (user == null || user.isEmpty()) user = "unknown";
        String ts = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(new Date());
        String uuid = UUID.randomUUID().toString();
        String suffix = index > 1 ? "_" + index : "";
        String ext = isVideo ? ".mp4" : ".jpg";
        return user + "_" + ts + "_" + uuid + suffix + ext;
    }

    private static void enqueue(Context context, Object media, String url, boolean isVideo, int index) {
        String filename = buildFilename(media, isVideo, index);

        Logcat.d("Downloading: " + filename + " from " + url.substring(0, Math.min(80, url.length())) + "...");

        String dir = downloadDir();
        if (dir.startsWith("content://")) {
            downloadToSafTree(context, Uri.parse(dir), filename, url, isVideo, index);
        } else {
            enqueueToPublicDir(context, dir, filename, url, index);
        }
    }

    private static void enqueueToPublicDir(Context context, String dir, String filename, String url, int index) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(filename);
        request.setDescription("Downloading Instagram media");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, dir + "/" + filename);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return;

        manager.enqueue(request);
        if (index <= 1) showToast(context, "Downloading " + filename);
    }

    private static void downloadToSafTree(Context context, Uri tree, String filename, String url, boolean isVideo, int index) {
        if (index <= 1) showToast(context, "Downloading " + filename);
        final String mime = isVideo ? "video/mp4" : "image/jpeg";
        new Thread(() -> {
            HttpURLConnection conn = null;
            InputStream in = null;
            OutputStream out = null;
            try {
                ContentResolver resolver = context.getContentResolver();
                Uri parentDoc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
                Uri fileDoc = DocumentsContract.createDocument(resolver, parentDoc, mime, filename);
                if (fileDoc == null) {
                    Logcat.e("createDocument returned null for " + filename);
                    return;
                }
                out = resolver.openOutputStream(fileDoc);
                if (out == null) {
                    Logcat.e("openOutputStream returned null for " + fileDoc);
                    return;
                }
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.connect();
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    Logcat.e("HTTP " + code + " fetching " + filename);
                    return;
                }
                in = conn.getInputStream();
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                out.flush();
                Logcat.d("Saved " + filename + " to " + fileDoc);
            } catch (Throwable t) {
                Logcat.e("SAF download failed for " + filename, t);
            } finally {
                try { if (in != null) in.close(); } catch (Throwable ignored) {}
                try { if (out != null) out.close(); } catch (Throwable ignored) {}
                if (conn != null) conn.disconnect();
            }
        }, "reseam-ig-download").start();
    }

    private static String downloadDir() {
        String value = ReseamSettings.getString(DOWNLOAD_FOLDER_KEY, DEFAULT_DOWNLOAD_DIR);
        if (value == null) return DEFAULT_DOWNLOAD_DIR;
        value = value.trim();
        return value.isEmpty() ? DEFAULT_DOWNLOAD_DIR : value;
    }

    private static void showToast(Context context, String message) {
        if (ReseamSettings.getBoolean(SHOW_TOAST_KEY, true)) {
            Ui.showToast(context, message);
        }
    }
}
