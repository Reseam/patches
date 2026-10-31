// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.download

import android.content.Context
import android.widget.Toast
import app.reseam.bundle.showToast
import app.reseam.instagram.settings.DownloadSettings

internal fun showToast(context: Context?, message: String) {
    if (DownloadSettings.showToast.value) context?.showToast(message, Toast.LENGTH_SHORT)
}
