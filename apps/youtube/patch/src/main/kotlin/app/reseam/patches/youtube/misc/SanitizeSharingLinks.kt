// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
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
        gate(YouTubeSettings.sanitizeSharingLinks) {
            shareSheetUrl.before {
                capture("shareUrl").assign(call(SharingLinks.sanitize, capture("shareUrl").cast(Type.String)))
            }
        }
        val extras = shareToAppExtras.all + shareChooserExtras.all
        check(extras.isNotEmpty()) { "No share intent extras" }
        extras.forEach {
            gate(YouTubeSettings.sanitizeSharingLinks) {
                it.captureArgumentAs("key", 1, Type.String).captureArgumentAs("value", 2, Type.String)
                    .before {
                        capture("value").assign(call(SharingLinks.sanitizeExtra, capture("key"), capture("value")))
                    }
            }
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

private const val INTENT = "android.content.Intent"

// Sharing to an app copies the extras the share command lists into the outgoing intent; the link is
// the text extra. The share sheet's own app targets address the app by class name, and the system
// chooser path logs the share with its endpoint.
private val shareToAppExtras = methods("share to app extras") {
    paramCount(2)
    param(1, "java.util.Map")
    returns(Type.Void)
    calls { owner(INTENT); name("setClassName") }
    calls { owner(INTENT); name("putExtra"); params(Type.String, Type.String) }
}.points("share text extra") {
    invokeVirtual { owner(INTENT); name("putExtra"); params(Type.String, Type.String) }
}

private val shareChooserExtras = methods("share chooser extras") {
    strings("YTShare_Logging_Share_Intent_Endpoint_Byte_Array")
    calls { owner(INTENT); name("putExtra"); params(Type.String, Type.String) }
}.points("share text extra") {
    invokeVirtual { owner(INTENT); name("putExtra"); params(Type.String, Type.String) }
}

object SharingLinks : ExtClass("app.reseam.youtube.misc.SharingLinks") {
    val sanitize by static(Type.String, returns = Type.String)
    val sanitizeExtra by static(Type.String, Type.String, returns = Type.String)
}
