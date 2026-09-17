// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.navbuttons;

import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;
import app.reseam.youtube.navigation.NavigationBar;
import app.reseam.youtube.navigation.NavigationBar.NavigationButton;

/** Runtime behavior for the bottom navigation bar and its toolbar buttons. */
public final class NavigationButtons {
    private NavigationButtons() {}

    public static void navigationTabCreated(NavigationButton button, View view) {
        boolean hidden = shouldHide(button);
        Logger.debug(() -> "Navigation button created: " + button + " hidden=" + hidden);
        if (hidden && view != null) view.setVisibility(View.GONE);
    }

    public static void hideNavigationButtonLabels(TextView view) {
        boolean hidden = Settings.getBoolean("hide_navigation_button_labels", false);
        Logger.debug(() -> "Navigation button labels hidden=" + hidden);
        if (hidden && view != null) view.setVisibility(View.GONE);
    }

    public static boolean useAnimatedNavigationButtons(boolean original) {
        boolean enabled = Settings.getBoolean("navigation_bar_animations", false);
        Logger.debug(() -> "Navigation button animations=" + enabled);
        return enabled;
    }

    public static boolean enableNarrowNavigationButton(boolean original) {
        boolean narrow = Settings.getBoolean("narrow_navigation_buttons", false);
        boolean result = narrow || original;
        Logger.debug(() -> "Narrow navigation buttons: " + original + " -> " + result);
        return result;
    }

    public static boolean useTranslucentNavigationStatusBar(boolean original) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return original;
        boolean disabled = Settings.getBoolean("disable_translucent_status_bar", false);
        boolean result = disabled ? false : original;
        Logger.debug(() -> "Translucent status bar: " + original + " -> " + result);
        return result;
    }

    public static boolean useTranslucentNavigationButtons(boolean original) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return original;

        boolean darkDisabled = Settings.getBoolean("disable_translucent_navigation_bar_dark", false);
        boolean lightDisabled = Settings.getBoolean("disable_translucent_navigation_bar_light", false);
        if (!darkDisabled && !lightDisabled) return original;
        if (darkDisabled && lightDisabled) return false;

        int mode = YouTubeContext.get().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean dark = mode == Configuration.UI_MODE_NIGHT_YES;
        boolean result = original && !(dark ? darkDisabled : lightDisabled);
        Logger.debug(() -> "Translucent navigation buttons: " + original + " -> " + result);
        return result;
    }

    public static void hideCreateButton(Enum<?> button, View view) {
        hideToolbarButton("create", button, view, "hide_toolbar_create_button");
    }

    public static void hideNotificationButton(Enum<?> button, View view) {
        hideToolbarButton("notification", button, view, "hide_toolbar_notification_button");
    }

    public static void hideSearchButton(Enum<?> button, View view) {
        hideToolbarButton("search", button, view, "hide_toolbar_search_button");
    }

    private static void hideToolbarButton(String kind, Enum<?> button, View view, String key) {
        boolean enabled = Settings.getBoolean(key, false);
        boolean matches = button != null && matches(kind, button.name());
        boolean hidden = enabled && matches;
        Logger.debug(() -> "Navigation toolbar " + kind + ": "
                + (button == null ? "null" : button.name()) + " hidden=" + hidden);
        if (hidden && view != null) view.setVisibility(View.GONE);
    }

    private static boolean matches(String kind, String name) {
        switch (kind) {
            case "create":
                return "CREATION_ENTRY".equals(name) || "FAB_CAMERA".equals(name);
            case "notification":
                return "TAB_ACTIVITY".equals(name) || "TAB_ACTIVITY_CAIRO".equals(name);
            case "search":
                return "SEARCH".equals(name)
                        || "SEARCH_CAIRO".equals(name)
                        || "SEARCH_BOLD".equals(name);
            default:
                return false;
        }
    }

    private static boolean shouldHide(NavigationButton button) {
        if (button == null) return false;
        switch (button) {
            case HOME:
                return Settings.getBoolean("hide_home_button", false);
            case SHORTS:
                return Settings.getBoolean("hide_shorts_button", true);
            case CREATE:
                return Settings.getBoolean("hide_create_button", true);
            case SUBSCRIPTIONS:
                return Settings.getBoolean("hide_subscriptions_button", false);
            case NOTIFICATIONS:
                return Settings.getBoolean("hide_notifications_button", false);
            case LIBRARY:
                return Settings.getBoolean("hide_library_button", false);
            default:
                return false;
        }
    }
}
