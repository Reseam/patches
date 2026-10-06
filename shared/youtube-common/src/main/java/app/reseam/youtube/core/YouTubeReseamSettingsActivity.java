// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.core;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.runtime.settings.ReseamSettingsScreen;

public final class YouTubeReseamSettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_DeviceDefault_NoActionBar);
        super.onCreate(savedInstanceState);
        ReseamSettings.init(this);
        setTitle("Reseam Settings");
        setContentView(ReseamSettingsScreen.build(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ReseamSettingsScreen.onActivityResult(this, requestCode, resultCode, data);
    }
}
