// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Shortcuts")

package app.reseam.tiktok.interaction

import android.app.Application
import android.content.pm.ShortcutManager
import app.reseam.runtime.Export

@Export
fun removeAll(application: Application) {
    application.getSystemService(ShortcutManager::class.java)?.removeAllDynamicShortcuts()
}
