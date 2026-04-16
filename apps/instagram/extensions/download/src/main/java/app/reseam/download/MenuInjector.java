// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

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
