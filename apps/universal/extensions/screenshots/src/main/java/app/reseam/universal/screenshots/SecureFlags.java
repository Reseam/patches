// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.universal.screenshots;

import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;

public final class SecureFlags {
    private SecureFlags() {}

    public static void addFlags(Window window, int flags) {
        window.addFlags(flags & ~WindowManager.LayoutParams.FLAG_SECURE);
    }

    public static void setFlags(Window window, int flags, int mask) {
        window.setFlags(flags & ~WindowManager.LayoutParams.FLAG_SECURE, mask);
    }

    public static void setAttributes(Window window, WindowManager.LayoutParams params) {
        window.setAttributes((WindowManager.LayoutParams) cleared(params));
    }

    public static void addView(WindowManager manager, View view, ViewGroup.LayoutParams params) {
        manager.addView(view, cleared(params));
    }

    public static void updateViewLayout(WindowManager manager, View view, ViewGroup.LayoutParams params) {
        manager.updateViewLayout(view, cleared(params));
    }

    public static void addView(ViewManager manager, View view, ViewGroup.LayoutParams params) {
        manager.addView(view, cleared(params));
    }

    public static void updateViewLayout(ViewManager manager, View view, ViewGroup.LayoutParams params) {
        manager.updateViewLayout(view, cleared(params));
    }

    public static void setSecure(SurfaceView view, boolean secure) {
        view.setSecure(false);
    }

    private static ViewGroup.LayoutParams cleared(ViewGroup.LayoutParams params) {
        if (params instanceof WindowManager.LayoutParams) {
            ((WindowManager.LayoutParams) params).flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
        }
        return params;
    }
}
