// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.settings.toggle

object YouTubeContext : ExtClass("app.reseam.youtube.core.YouTubeContext") {
    val init by static(Type.Context)
}

// Keys stay the ones YouTube shipped them under, which the shared extensions read.
object YouTubeCommonSettings {
    val debugLogging by toggle(
        "Debug logging",
        summary = "Writes what the Reseam patches are doing to the Android log.",
        default = false,
        key = "you_tube_settings.debug_logging",
    )
    val sanitizeSharingLinks by toggle(
        "Sanitize sharing links",
        summary = "Strips the tracking parameters YouTube adds to a shared link.",
        default = true,
        key = "you_tube_settings.sanitize_sharing_links",
    )
    val forceOriginalAudio by toggle(
        "Force original audio",
        summary = "Prefers the original audio track over dubbed tracks.",
        default = true,
        key = "you_tube_settings.force_original_audio",
    )
}
