// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

final class CarouselDialog {
    interface Callbacks {
        void onCurrent();
        void onAll();
    }

    private CarouselDialog() {}

    static void show(Context context, int totalSlides, int currentSlide, Callbacks callbacks) {
        if (context == null) {
            callbacks.onAll();
            return;
        }

        boolean dark = isDark(context);
        int bgColor = dark ? 0xFF262626 : 0xFFFFFFFF;
        int textColor = dark ? 0xFFFFFFFF : 0xFF000000;
        int dividerColor = dark ? 0xFF363636 : 0xFFEFEFEF;
        int handleColor = dark ? 0xFF555555 : 0xFFC7C7C7;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        float r = dp(context, 14);
        bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        root.setBackground(bg);

        // Drag handle
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(handleColor);
        handleBg.setCornerRadius(dp(context, 2));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(context, 36), dp(context, 4));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.topMargin = dp(context, 8);
        handleLp.bottomMargin = dp(context, 12);
        root.addView(handle, handleLp);

        boolean hasCurrent = currentSlide >= 0 && currentSlide < totalSlides;
        if (hasCurrent) {
            root.addView(row(context, "Current (" + (currentSlide + 1) + "/" + totalSlides + ")", textColor, () -> {
                dialog.dismiss();
                callbacks.onCurrent();
            }));
            root.addView(divider(context, dividerColor));
        }
        root.addView(row(context, "All " + totalSlides + " items", textColor, () -> {
            dialog.dismiss();
            callbacks.onAll();
        }));
        root.addView(divider(context, dividerColor));
        root.addView(row(context, "Cancel", textColor, dialog::dismiss));
        root.setPadding(0, 0, 0, dp(context, 8));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new GradientDrawable());
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }
        dialog.show();
    }

    private static View row(Context context, String label, int textColor, Runnable action) {
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tv.setGravity(Gravity.CENTER);
        int padV = dp(context, 16);
        tv.setPadding(0, padV, 0, padV);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setClickable(true);
        tv.setFocusable(true);

        TypedValue ripple = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)) {
            tv.setBackgroundResource(ripple.resourceId);
        }
        tv.setOnClickListener(v -> action.run());
        return tv;
    }

    private static View divider(Context context, int color) {
        View v = new View(context);
        v.setBackgroundColor(color);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    private static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private static boolean isDark(Context context) {
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }
}
