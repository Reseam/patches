// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.theme;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;

public final class CustomBranding {
    private static final String[] ICON_STYLES = { "original", "reseam" };
    private static final int NAME_COUNT = 4;

    private CustomBranding() {}

    public static void setBranding() {
        try {
            Context context = YouTubeContext.get();
            PackageManager manager = context.getPackageManager();
            String packageName = context.getPackageName();
            String name = Settings.getString("custom_branding_name", "2");
            String icon = Settings.getString("custom_branding_icon", "original");
            final String selectedName = java.util.Arrays.asList("1", "2", "3", "4").contains(name) ? name : "2";
            final String selectedIcon = java.util.Arrays.asList(ICON_STYLES).contains(icon) ? icon : "original";
            String selectedAlias = packageName + ".reseam_" + selectedIcon + "_" + selectedName;
            // Enable the destination first so switching presets always leaves a launcher entry.
            setEnabled(manager, new ComponentName(packageName, selectedAlias), true);
            for (String style : ICON_STYLES) {
                for (int index = 1; index <= NAME_COUNT; index++) {
                    String className = packageName + ".reseam_" + style + "_" + index;
                    boolean enabled = style.equals(selectedIcon) && Integer.toString(index).equals(selectedName);
                    if (!enabled) setEnabled(manager, new ComponentName(packageName, className), false);
                }
            }
            Logger.debug(() -> "Custom branding: name=" + selectedName + " icon=" + selectedIcon);
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

    public static void setNotificationIcon(Notification.Builder builder) {
        try {
            if ("original".equals(Settings.getString("custom_branding_icon", "original"))) return;
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
