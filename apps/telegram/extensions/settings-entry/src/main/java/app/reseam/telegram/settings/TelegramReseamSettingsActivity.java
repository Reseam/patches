// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.telegram.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import app.reseam.runtime.settings.ReseamSettings;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;

public final class TelegramReseamSettingsActivity extends Activity {
    private static final class Toggle {
        final String key;
        final String title;
        final String summary;
        final boolean defaultValue;
        Toggle(String key, String title, String summary, boolean defaultValue) {
            this.key = key; this.title = title; this.summary = summary; this.defaultValue = defaultValue;
        }
    }

    private static final Toggle[] TOGGLES = new Toggle[] {
        new Toggle("ads.hide_sponsored", "Hide sponsored messages", null, true),
        new Toggle("update.disable_auto_check", "Disable auto-update", null, true),
        new Toggle("premium.unlock_client", "Unlock Premium",
            "Server-checked features still need a real subscription.", true),
        new Toggle("privacy.hide_typing", "Hide typing indicator", null, true),
        new Toggle("downloads.boost", "Boost download speed", null, true),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ReseamSettings.init(this);
        setTitle("Reseam Settings");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        HeaderCell header = new HeaderCell(this);
        header.setText("Telegram tweaks");
        root.addView(header, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < TOGGLES.length; i++) {
            Toggle t = TOGGLES[i];
            boolean isLast = i == TOGGLES.length - 1;
            TextCheckCell cell = new TextCheckCell(this);
            boolean checked = ReseamSettings.getBoolean(t.key, t.defaultValue);
            if (t.summary != null) {
                cell.setTextAndValueAndCheck(t.title, t.summary, checked, true, !isLast);
            } else {
                cell.setTextAndCheck(t.title, checked, !isLast);
            }
            cell.setOnClickListener(v -> {
                boolean now = !((TextCheckCell) v).isChecked();
                ((TextCheckCell) v).setChecked(now);
                ReseamSettings.setBoolean(t.key, now);
            });
            root.addView(cell, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }
}
