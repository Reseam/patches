// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.theme;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;

public final class CustomBranding {
    private static final String NAME_KEY = "custom_branding_name";
    private static final String ICON_KEY = "custom_branding_icon";

    private CustomBranding() {}

    public static void setBranding() {
        try {
            Context context = YouTubeContext.get();
            PackageManager manager = context.getPackageManager();
            String packageName = context.getPackageName();
            String name = Settings.getChoice(NAME_KEY);
            String icon = Settings.getChoice(ICON_KEY);
            String aliasPrefix = packageName + ".reseam_";
            String selectedAlias = aliasPrefix + icon + "_" + name;
            // Enable the destination first so switching presets always leaves a launcher entry.
            setEnabled(manager, new ComponentName(packageName, selectedAlias), true);
            ActivityInfo[] activities = manager.getPackageInfo(packageName,
                    PackageManager.GET_ACTIVITIES | PackageManager.MATCH_DISABLED_COMPONENTS).activities;
            for (ActivityInfo activity : activities) {
                if (activity.name.startsWith(aliasPrefix) && !activity.name.equals(selectedAlias)) {
                    setEnabled(manager, new ComponentName(packageName, activity.name), false);
                }
            }
            Logger.debug(() -> "Custom branding: name=" + name + " icon=" + icon);
        } catch (Throwable ex) {
            Logger.error(() -> "Custom branding failed: " + ex.getClass().getSimpleName());
        }
    }

    private static void setEnabled(PackageManager manager, ComponentName component, boolean enabled) {
        int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        if (manager.getComponentEnabledSetting(component) != state) {
            manager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);
        }
    }

    /** The Reseam glyph only matches the Reseam icon; a custom icon keeps YouTube's notification icon. */
    public static void setNotificationIcon(Notification.Builder builder) {
        try {
            if (!"reseam".equals(Settings.getChoice(ICON_KEY))) return;
            Context context = YouTubeContext.get();
            int id = context.getResources().getIdentifier(
                    "reseam_notification_icon", "drawable", context.getPackageName());
            if (id != 0) builder.setSmallIcon(id).setColor(Color.TRANSPARENT);
            Logger.debug(() -> "Notification icon set");
        } catch (Throwable ex) {
            Logger.error(() -> "Notification icon failed: " + ex.getClass().getSimpleName());
        }
    }
}
