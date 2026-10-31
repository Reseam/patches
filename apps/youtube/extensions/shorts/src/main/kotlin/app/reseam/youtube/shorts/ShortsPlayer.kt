// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShortsPlayer")

package app.reseam.youtube.shorts

import app.reseam.runtime.Export
import app.reseam.youtube.navigation.Commands
import app.reseam.youtube.navigation.NavigationBar.NavigationButton

/** Returns true when the Short was handed to the regular player instead. */
@Export
fun openInRegularPlayer(videoId: String?): Boolean {
    // The launcher shortcut opens the Shorts feed without a video.
    if (videoId.isNullOrEmpty()) return false
    // Choosing the Shorts tab is choosing the Shorts player.
    if (NavigationButton.selectedNavigationButton == NavigationButton.SHORTS) return false
    return Commands.openVideo(videoId)
}
