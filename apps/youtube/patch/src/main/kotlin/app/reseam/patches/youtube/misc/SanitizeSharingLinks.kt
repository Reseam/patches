// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val sanitizeSharingLinks = patch("Sanitize sharing links") {
    description("Strips the tracking parameters YouTube adds to a link before you share it.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, "Privacy", YouTubeSettings.sanitizeSharingLinks))

    execute {
        shareSheetUrl.before(YouTubeSettings.sanitizeSharingLinks) {
            capture("shareUrl").assign(call(SharingLinks.sanitize, capture("shareUrl").cast(Type.String)))
        }
    }
}

// The copy-link action is the one place the share URL exists as a plain String register.
val shareSheetCopyLink = method("shareSheetCopyLink") {
    strings("text/plain")
    returns(Type.Void)
    calls { name("newPlainText") }
}

// ClipData's second argument is the shared URL; the label is independent of it.
val shareSheetUrl = shareSheetCopyLink.point {
    invokeStatic { owner("android.content.ClipData"); name("newPlainText") }
}.captureArgumentAs("shareUrl", 1, Type.CharSequence)

object SharingLinks : ExtClass("app.reseam.youtube.misc.SharingLinks") {
    val sanitize = static("sanitize", Type.String, returns = Type.String)
}
