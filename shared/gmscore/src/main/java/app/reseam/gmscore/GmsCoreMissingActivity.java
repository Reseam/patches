// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.gmscore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;

/**
 * Explains that GmsCore is missing. Without it, the app's sign-in flow can close the app's whole
 * task at launch (YouTube calls finishAffinity), so this runs in a task of its own.
 */
public final class GmsCoreMissingActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        new AlertDialog.Builder(this, night
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle("GmsCore is not installed")
                .setMessage("This app was patched to sign in through GmsCore ("
                        + GmsCoreSupport.gmsCorePackageName() + "), which is not installed. Install it, "
                        + "open it once and grant the permissions it asks for, then open this app again.")
                .setPositiveButton("Get GmsCore", (dialog, which) ->
                        GmsCoreSupport.open(this, Uri.parse("https://github.com/revanced/gmscore/releases/latest")))
                .setNegativeButton("Close", null)
                .setOnDismissListener(dialog -> finish())
                .show();
    }
}
