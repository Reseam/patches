// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("YouTubeMusicPackage")

package app.reseam.soundconnect.youtubemusic

import android.app.Application
import android.content.pm.PackageManager
import app.reseam.runtime.Export

internal const val PLAY_STORE_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"

private var packageManager: PackageManager? = null

@Export fun reseamPackage(): String = ""

@Export
fun init(application: Application) {
    packageManager = application.packageManager
}

@Export
fun resolve(packageName: String): String {
    if (packageName != PLAY_STORE_YOUTUBE_MUSIC) return packageName
    val reseam = reseamPackage()
    return if (isInstalled(reseam)) reseam else packageName
}

private fun isInstalled(packageName: String): Boolean {
    // Content providers start before Application.onCreate, where init runs.
    val manager = packageManager ?: return false
    return try {
        manager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
