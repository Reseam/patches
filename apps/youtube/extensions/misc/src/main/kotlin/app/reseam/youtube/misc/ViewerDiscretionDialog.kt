// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("ViewerDiscretionDialog")

package app.reseam.youtube.misc

import android.app.AlertDialog
import android.view.WindowManager
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug

private val viewerDiscretionStatuses =
    setOf(
        "AGE_CHECK_REQUIRED",
        "AGE_VERIFICATION_REQUIRED",
        "CONTENT_CHECK_REQUIRED",
        "LOGIN_REQUIRED",
    )
@Volatile private var playabilityStatus = ""

@Export
fun setPlayabilityStatus(status: Enum<*>?) {
    playabilityStatus = status?.name ?: ""
}

@Export
fun confirm(dialog: AlertDialog) {
    if (playabilityStatus !in viewerDiscretionStatuses) return
    if (!dialog.isShowing) dialog.show()
    val confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE) ?: return
    dialog.window?.let { window ->
        // Hide the dialog during its roughly one-second dismissal animation.
        window.attributes =
            window.attributes.apply {
                height = 0
                width = 0
            }
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    Logger.debug { "Confirming viewer discretion dialog for $playabilityStatus" }
    confirm.callOnClick()
}

// The modern dialog has no reachable confirm button.
@Export
fun useModernDialog(original: Boolean): Boolean =
    original && playabilityStatus !in viewerDiscretionStatuses
