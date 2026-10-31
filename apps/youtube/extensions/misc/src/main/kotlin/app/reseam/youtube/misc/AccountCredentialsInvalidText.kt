// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("AccountCredentialsInvalidText")

package app.reseam.youtube.misc

import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.isNetworkConnected
import app.reseam.youtube.settings.AdvancedSettings

@Export
fun getOfflineNetworkErrorString(original: String): String {
    if (!AdvancedSettings.accountCredentialsInvalidText.value) return original
    if (!isNetworkConnected()) return original
    val context = YouTubeContext.applicationContext
    val id = context.identifier("microg_offline_account_login_error", "string")
    return if (id == 0) original else "\n${context.getString(id)}"
}
