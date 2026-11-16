// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.links

import app.reseam.instagram.links.bindings.ShareLinks
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.instagram.settings.LinkSettings
import app.reseam.patch.Type
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.redirectTo
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val EXTRA_TEXT = "android.intent.extra.TEXT"

val cleanShareLinks = patch(InstagramPatchStrings.cleanShareLinks) {
    description(InstagramPatchStrings.cleanShareLinksDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(
        instagramSettings,
        section(InstagramStrings.sectionLinks, LinkSettings.cleanShareLinks, LinkSettings.shareLinkProvider, LinkSettings.customShareLinkHost),
    )

    execute {
        // Links reach the app from several share-url responses, some read through generic model accessors,
        // so they are rewritten where they leave it: the clipboard and the text of share intents.
        bytecode.redirectCalls("android.content.ClipboardManager", "setPrimaryClip", ShareLinks.setPrimaryClip)
        bytecode.redirectCalls("android.text.ClipboardManager", "setText", ShareLinks.setText)
        shareTextWriters.points("bundleShareText") {
            invokeVirtual { name("putString"); params(Type.String, Type.String) }
            argument(1) { string(EXTRA_TEXT) }
        }.forEach { redirectTo(ShareLinks.putString) }
        shareTextWriters.points("intentShareText") {
            invokeVirtual { owner("android.content.Intent"); name("putExtra"); params(Type.String, Type.String) }
            argument(1) { string(EXTRA_TEXT) }
        }.forEach { redirectTo(ShareLinks.putExtra) }
    }
}

private val shareTextWriters = methods("shareTextWriters") { strings(EXTRA_TEXT) }
