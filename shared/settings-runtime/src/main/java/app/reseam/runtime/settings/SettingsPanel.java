// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.runtime.settings;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.window.OnBackInvokedDispatcher;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows the settings pages in a full-screen dialog over the app's current screen. A dialog needs no
 * activity in the manifest, so the same build works installed or mounted over the installed app,
 * where the system keeps the installed manifest.
 */
final class SettingsPanel implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ReseamSettings";
    private static final String ROOT_PAGE = "";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static SettingsPanel instance;

    private WeakReference<Activity> resumed = new WeakReference<>(null);
    private Activity host;
    private Dialog dialog;
    private FrameLayout frame;
    private final ArrayDeque<View> pages = new ArrayDeque<>();
    private final List<String> pageIds = new ArrayList<>();
    private Map<String, ?> opened;
    private List<String> reopen;
    private Class<?> reopenIn;

    private SettingsPanel() {}

    static void track(Application app) {
        if (instance != null) return;
        instance = new SettingsPanel();
        app.registerActivityLifecycleCallbacks(instance);
    }

    static void open() {
        SettingsPanel panel = instance;
        if (panel == null) throw new IllegalStateException("ReseamSettings.init has not run");
        MAIN.post(() -> panel.show(Collections.singletonList(ROOT_PAGE)));
    }

    void push(String pageId) {
        View page = ReseamSettingsScreen.page(dialog.getContext(), pageId, this);
        pages.push(page);
        pageIds.add(pageId);
        display(page);
    }

    void back() {
        if (pages.size() == 1) {
            dialog.dismiss();
            return;
        }
        pages.pop();
        pageIds.remove(pageIds.size() - 1);
        display(pages.peek());
    }

    private void display(View page) {
        frame.removeAllViews();
        frame.addView(page);
        // Insets reach only the views attached when the window first lays out.
        frame.requestApplyInsets();
    }

    private void show(List<String> ids) {
        Activity activity = resumed.get();
        if (dialog != null) return;
        if (activity == null) {
            Log.e(TAG, "Cannot open settings: no screen of the app is in the foreground");
            return;
        }
        if (opened == null) opened = new HashMap<>(ReseamSettings.prefs().getAll());
        host = activity;
        dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar) {
            @Override
            public void onBackPressed() {
                back();
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Hosts that opt in to predictive Back never call onBackPressed.
            dialog.getOnBackInvokedDispatcher()
                    .registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);
        }
        dialog.setOnDismissListener(d -> closed());
        frame = new FrameLayout(dialog.getContext());
        dialog.setContentView(frame);
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        for (String id : ids) push(id);
        dialog.show();
    }

    private void closed() {
        Activity activity = host;
        release();
        RestartPrompt.offer(activity, opened);
        opened = null;
    }

    private void release() {
        host = null;
        dialog = null;
        frame = null;
        pages.clear();
        pageIds.clear();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        resumed = new WeakReference<>(activity);
        if (reopen == null || activity.getClass() != reopenIn) return;
        List<String> ids = reopen;
        reopen = null;
        reopenIn = null;
        show(ids);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity != host) return;
        // The dismiss listener runs later, after a recreated host has already resumed.
        dialog.setOnDismissListener(null);
        dialog.dismiss();
        // A recreated host gets the same pages back; a finished one takes the panel with it.
        if (activity.isChangingConfigurations()) {
            reopen = new ArrayList<>(pageIds);
            reopenIn = activity.getClass();
        } else {
            opened = null;
        }
        release();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
}
