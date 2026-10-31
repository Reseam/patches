// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("UserAgentClientSpoof")

package app.reseam.youtube.spoof

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtubespoof.settings.SpoofSettings

@Export
fun rewritePackageName(original: String?, stockPackageName: String): String? {
    if (!SpoofSettings.userAgentClientSpoof.value) return original
    Logger.debug { "User-agent package name spoofed to $stockPackageName" }
    return stockPackageName
}
