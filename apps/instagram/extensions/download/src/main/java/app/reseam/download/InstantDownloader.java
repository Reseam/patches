// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.instagram.download;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** UI bridge for the current Instants viewer. */
public final class InstantDownloader {
    private static View button;
    private static Object media;
    private static Object currentViewer;

    private InstantDownloader() {}

    public static void attach(Object viewer, View root) {
        if (viewer == null || root == null) return;
        detach(currentViewer);
        currentViewer = viewer;
        root.post(() -> attachWhenReady(viewer, root));
    }

    private static void attachWhenReady(Object viewer, View root) {
        if (viewer != currentViewer || !root.isAttachedToWindow()) return;
        View content = root.getRootView().findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) return;
        FrameLayout parent = (FrameLayout) content;
        Context context = parent.getContext();
        TextView control = new TextView(context);
        control.setText("Download");
        control.setTextColor(Color.WHITE);
        control.setTextSize(14);
        control.setGravity(Gravity.CENTER);
        int horizontal = dp(context, 12);
        control.setPadding(horizontal, dp(context, 8), horizontal, dp(context, 8));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xCC202020);
        background.setCornerRadius(dp(context, 20));
        control.setBackground(background);
        control.setElevation(dp(context, 8));
        control.setContentDescription("Download instant");
        control.setVisibility(media == null ? View.GONE : View.VISIBLE);
        control.setOnClickListener(v -> {
            Object selected = media;
            if (selected != null) DownloadEnqueuer.download(selected, context);
        });
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        params.setMargins(dp(context, 12), dp(context, 64), dp(context, 12), 0);
        parent.addView(control, params);
        button = control;
    }

    public static void showItem(Object item) {
        Object selectedMedia = InstantMedia.media(item);
        media = selectedMedia;
        View control = button;
        if (control != null) control.post(() -> control.setVisibility(selectedMedia == null ? View.GONE : View.VISIBLE));
    }

    public static void detach(Object viewer) {
        if (viewer != currentViewer) return;
        View control = button;
        button = null;
        media = null;
        currentViewer = null;
        if (control != null && control.getParent() instanceof ViewGroup) {
            ((ViewGroup) control.getParent()).removeView(control);
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
