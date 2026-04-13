/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.settings;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.instagram.base.activity.IgActivity;

import dev.stitch.runtime.settings.StitchSettings;
import dev.stitch.runtime.settings.StitchSettingsScreen;

public final class InstagramStitchSettingsActivity extends IgActivity {
    private static final String INSTAGRAM_MAIN_ACTIVITY = "com.instagram.mainactivity.InstagramMainActivity";
    private static final String IGDS_PRISM_COLORS = "IgdsPrismSemanticColorsExperiment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyInstagramActivityTheme();
        super.onCreate(savedInstanceState);
        StitchSettings.init(this);
        setTitle("Stitch Settings");
        setContentView(StitchSettingsScreen.build(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (StitchSettingsScreen.onActivityResult(this, requestCode, resultCode, data)) {
            setContentView(StitchSettingsScreen.build(this));
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
