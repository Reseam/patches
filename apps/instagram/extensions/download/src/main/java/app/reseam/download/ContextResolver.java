// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.content.Context;

final class ContextResolver {
    private ContextResolver() {}

    static Context best(Context explicit, Object... owners) {
        Context safe = safe(explicit);
        if (safe != null) return safe;
        return fromObjects(owners);
    }

    static Context fromObjects(Object... owners) {
        for (Object owner : owners) {
            Context context = find(owner);
            if (context != null) return safe(context);
        }
        return app();
    }

    static Context find(Object object) {
        return object instanceof Context ? (Context) object : null;
    }

    static Context safe(Context context) {
        try {
            if (context != null && context.getApplicationContext() != null) {
                return context.getApplicationContext();
            }
        } catch (Throwable ignored) {}
        return app();
    }

    private static boolean isContext(Object object) {
        return object instanceof Context;
    }

    private static Context app() {
        try {
            Object activityThread = Class.forName("android.app.ActivityThread")
                    .getMethod("currentActivityThread")
                    .invoke(null);
            return (Context) activityThread.getClass()
                    .getMethod("getApplication")
                    .invoke(activityThread);
        } catch (Throwable t) {
            Logcat.e("getAppContext failed", t);
            return null;
        }
    }
}
