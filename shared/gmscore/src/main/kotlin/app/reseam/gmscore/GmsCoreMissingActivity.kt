// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.gmscore

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import app.reseam.bundle.isNightMode
import app.reseam.gmscore.settings.GmsCoreStrings
import app.reseam.runtime.Export

/** Runs in its own task because the app's sign-in flow can finish the app's whole task. */
@Export
class GmsCoreMissingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme =
            if (resources.isNightMode()) android.R.style.Theme_DeviceDefault_Dialog_Alert
            else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
        AlertDialog.Builder(this, theme)
            .setTitle(GmsCoreStrings.gmscoreMissingTitle)
            .setMessage(GmsCoreStrings.gmscoreMissingMessage(gmsCorePackageName()))
            .setPositiveButton(GmsCoreStrings.gmscoreGet) { _, _ ->
                openScreen(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://reseam.app/download/#gmscore"),
                    )
                )
            }
            .setNegativeButton(GmsCoreStrings.gmscoreClose, null)
            .setOnDismissListener { finish() }
            .show()
    }
}
