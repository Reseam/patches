// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.content.Context;

final class FeedClickHandler {
    private FeedClickHandler() {}

    static boolean handleListener(Object listener) {
        if (listener == null) return false;

        Object option = MediaMeta.listenerDownloadOption(listener);
        if (!DownloadOption.isDownload(option)) return false;

        Context context = ContextResolver.fromObjects(listener);
        try {
            Object media = MediaMeta.listenerMedia(listener);
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
}
