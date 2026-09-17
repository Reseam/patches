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
            String selectedName = Settings.getString("custom_branding_name", "2");
            String selectedIcon = Settings.getString("custom_branding_icon", "original");
            boolean found = false;
            for (String style : ICON_STYLES) {
                for (int index = 1; index <= NAME_COUNT; index++) {
                    String className = packageName + ".reseam_" + style + "_" + index;
                    boolean enabled = style.equals(selectedIcon) && Integer.toString(index).equals(selectedName);
                    manager.setComponentEnabledSetting(
                            new ComponentName(packageName, className),
                            enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                    found |= enabled;
                }
            }
            if (!found) {
                manager.setComponentEnabledSetting(
                        new ComponentName(packageName, packageName + ".reseam_reseam_2"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
            Logger.debug(() -> "Custom branding: name=" + selectedName + " icon=" + selectedIcon);
        } catch (Throwable ex) {
            Logger.error(() -> "Custom branding failed: " + ex.getClass().getSimpleName());
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
