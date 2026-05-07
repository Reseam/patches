// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.telegram.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;

import app.reseam.runtime.settings.ReseamSettings;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.UItem;

public final class TelegramSettingsEntry {
    private static final String TAG = "ReseamSettings";
    private static volatile Drawable cachedLogo;

    private TelegramSettingsEntry() {}

    public static void init(Context ctx) {
        ReseamSettings.init(ctx);
    }

    /** Hook target: invoked at the tail of SettingsActivity.fillItems. */
    public static void appendReseamItem(ArrayList<UItem> items, BaseFragment fragment) {
        if (items == null || fragment == null) return;
        try {
            Activity a = fragment.getParentActivity();
            if (a == null) return;
            TextCell cell = new TextCell(a);
            Drawable logo = getLogo(a);
            if (logo != null) cell.setTextAndIcon("Reseam", logo, false);
            else cell.setText("Reseam", false);
            cell.setOnClickListener(v -> a.startActivity(new Intent(a, TelegramReseamSettingsActivity.class)));
            items.add(UItem.asCustom(0, cell));
        } catch (Throwable t) {
            Log.w(TAG, "appendReseamItem failed", t);
        }
    }

    private static Drawable getLogo(Context ctx) {
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
