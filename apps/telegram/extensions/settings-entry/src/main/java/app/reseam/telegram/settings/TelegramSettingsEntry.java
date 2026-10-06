// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.telegram.settings;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.io.InputStream;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.runtime.settings.ReseamSettingsScreen;

public final class TelegramSettingsEntry {
    private static volatile Drawable cachedLogo;

    private TelegramSettingsEntry() {}

    public static void init(Context ctx) {
        ReseamSettings.init(ctx);
    }

    // Built from Telegram's own cell and list item, so the body is emitted by the settings host.
    public static Object reseamItem(Activity activity) {
        throw new UnsupportedOperationException("implemented by the settings host patch");
    }

    public static View.OnClickListener opener() {
        return v -> ReseamSettingsScreen.open();
    }

    public static Drawable logo(Context ctx) {
        Drawable cached = cachedLogo;
        if (cached != null) return cached;
        try (InputStream in = ctx.getAssets().open("reseam/logo.png")) {
            Bitmap bmp = BitmapFactory.decodeStream(in);
            if (bmp != null) {
                BitmapDrawable d = new BitmapDrawable(ctx.getResources(), bmp);
                cachedLogo = d;
                return d;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
