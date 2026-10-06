// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.ExtClass
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.gate

fun PatchRuntime.sanitizeSharedLinks(setting: ToggleSetting) {
    gate(setting) {
        shareSheetUrl.before {
            capture("shareUrl").assign(call(SharingLinks.sanitize, capture("shareUrl").cast(Type.String)))
        }
    }
    val extras = shareToAppExtras.all + shareChooserExtras.all
    check(extras.isNotEmpty()) { "No share intent extras" }
    extras.forEach {
        gate(setting) {
            it.captureArgumentAs("key", 1, Type.String).captureArgumentAs("value", 2, Type.String)
                .before {
                    capture("value").assign(call(SharingLinks.sanitizeExtra, capture("key"), capture("value")))
                }
        }
    }
}

// The copy-link action is the one place the share URL exists as a plain String register.
private val shareSheetCopyLink = method("shareSheetCopyLink") {
    strings("text/plain")
    returns(Type.Void)
    calls { name("newPlainText") }
}

// ClipData's second argument is the shared URL; the label is independent of it.
private val shareSheetUrl = shareSheetCopyLink.point {
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

private object SharingLinks : ExtClass("app.reseam.youtube.links.SharingLinks") {
    val sanitize by static(Type.String, returns = Type.String)
    val sanitizeExtra by static(Type.String, Type.String, returns = Type.String)
}
