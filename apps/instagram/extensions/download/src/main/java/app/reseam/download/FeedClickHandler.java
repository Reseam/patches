// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.content.Context;

final class FeedClickHandler {
    private FeedClickHandler() {}

    static boolean handle(Object media, Object option, Context context, int currentIndex) {
        if (!DownloadOption.isDownload(option)) return false;

        Context safe = ContextResolver.safe(context);
        try {
            if (media != null) {
                DownloadEnqueuer.download(media, safe, currentIndex);
            } else {
                Ui.showToast(safe, "Could not extract media URL");
            }
        } catch (Throwable t) {
            Logcat.e("handleFeedMenuClick failed", t);
            Ui.showToast(safe, "Download failed");
        }
        return true;
    }
}
