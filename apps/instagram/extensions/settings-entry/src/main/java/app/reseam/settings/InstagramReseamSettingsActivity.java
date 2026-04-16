// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.settings;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.instagram.base.activity.IgActivity;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.runtime.settings.ReseamSettingsScreen;

public final class InstagramReseamSettingsActivity extends IgActivity {
    private static final String INSTAGRAM_MAIN_ACTIVITY = "com.instagram.mainactivity.InstagramMainActivity";
    private static final String IGDS_PRISM_COLORS = "IgdsPrismSemanticColorsExperiment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyInstagramActivityTheme();
        super.onCreate(savedInstanceState);
        ReseamSettings.init(this);
        setTitle("Reseam Settings");
        setContentView(ReseamSettingsScreen.build(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ReseamSettingsScreen.onActivityResult(this, requestCode, resultCode, data)) {
            setContentView(ReseamSettingsScreen.build(this));
        }
    }

    private void applyInstagramActivityTheme() {
        int theme = 0;
        try {
            ComponentName main = new ComponentName(this, INSTAGRAM_MAIN_ACTIVITY);
            ActivityInfo info = getPackageManager().getActivityInfo(main, 0);
            theme = info.theme;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (theme != 0) {
            setTheme(theme);
        }
        try {
            String resourcePackage = getResources().getResourcePackageName(theme);
            int igdsColors = getResources().getIdentifier(IGDS_PRISM_COLORS, "style", resourcePackage);
            if (igdsColors != 0) {
                getTheme().applyStyle(igdsColors, true);
            }
        } catch (RuntimeException ignored) {
        }
    }
}
