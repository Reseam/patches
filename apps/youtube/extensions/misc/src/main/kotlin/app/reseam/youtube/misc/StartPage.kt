// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("StartPage")

package app.reseam.youtube.misc

import android.content.Intent
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.AppearanceSettings

private var applied = false
private const val INTENT_ACTION_PREFIX = "com.google.android.youtube.action."

@Export
fun overrideBrowseId(original: String): String {
    val page = AppearanceSettings.changeStartPage.value
    val isBrowsePage = page.isNotEmpty() && !page.startsWith(INTENT_ACTION_PREFIX)
    if (!isBrowsePage || !claim()) return original
    Logger.debug { "Changing browse id to $page" }
    return page
}

@Export
fun overrideIntentAction(intent: Intent?) {
    val page = AppearanceSettings.changeStartPage.value
    if (page.isEmpty() || !page.startsWith(INTENT_ACTION_PREFIX)) return
    // Shortcuts and widgets supply their own actions and must keep them.
    if (intent?.action != Intent.ACTION_MAIN || !claim()) return
    Logger.debug { "Changing intent action to $page" }
    intent.action = page
}

@Synchronized
private fun claim(): Boolean =
    when {
        AppearanceSettings.changeStartPageAlways.value -> true
        applied -> false
        else -> {
            applied = true
            true
        }
    }
