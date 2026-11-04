// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.youtube.core.bindings.YouTubeContext

/**
 * Gives the extensions the app's context. Settings hosts depend on it, so its entry code runs ahead of every patch's,
 * and extensions can log and look up resources from their first hook on.
 */
val youTubeContext = patch {
    execute {
        appEntry { YouTubeContext.init(application) }
    }
}
