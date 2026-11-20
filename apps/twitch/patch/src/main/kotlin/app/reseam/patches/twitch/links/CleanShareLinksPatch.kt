// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.links

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.links.bindings.ShareLinks
import app.reseam.twitch.settings.LinkSettings
import app.reseam.twitch.settings.TwitchStrings

// Twitch's R8 rules keep the names of its models and their getters.
private const val SHAREABLE_CONTENT = "tv.twitch.android.shared.twitch.share.sheet.pub.TwitchShareableContent"
private const val SHAREABLE_VIDEO = $$"tv.twitch.android.shared.twitch.share.sheet.pub.TwitchShareableContent$VOD"
private const val EXTRA_TEXT = "android.intent.extra.TEXT"

val cleanShareLinks = patch(TwitchPatchStrings.cleanShareLinks) {
    description(TwitchPatchStrings.cleanShareLinksDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Advanced, TwitchStrings.sectionPrivacy, LinkSettings.cleanShareLinks))

    execute {
        gate(LinkSettings.cleanShareLinks) {
            shareText.after { returnValue(ShareLinks.clean(result)) }
            chatShareLink.after { capture("link").assign(ShareLinks.clean(capture("link"))) }
            shareTextDataConstructor.before {
                param(0).assign(ShareLinks.clean(param(0)))
                param(1).assign(ShareLinks.clean(param(1)))
            }

            val intentTexts = sharedTexts.all
            require(intentTexts.isNotEmpty()) { "The app no longer shares text through $EXTRA_TEXT" }
            intentTexts.forEach { text ->
                text.captureArgumentAs("text", 2).before { capture("text").assign(ShareLinks.clean(capture("text"))) }
            }
            log.info("Cleaned the share sheet, Send to chat, the older share menus and ${intentTexts.size} share intents")
        }
    }
}

// The share sheet's text for any shared content.
private val shareText = method("shareText") {
    calls { owner(SHAREABLE_VIDEO); name("shareableText") }
    returns(Type.String)
}

private val chatShareLink = method("chatShare") { strings("send_to_chat") }
    .point("shareableLink") { invokeVirtual { owner(SHAREABLE_CONTENT); name("getShareableLink") } }
    .next { resultOf(Type.String) }
    .captureAs("link")

// The older share menus and Whisper shares.
private val shareTextDataConstructor = klass("shareTextData") { strings("ShareTextData(url=") }
    .method("<init>") { params(Type.String, Type.String) }

private val sharedTexts = methods("textShares") { strings(EXTRA_TEXT) }
    .points("sharedTexts") {
        invokeVirtual { owner("android.content.Intent"); name("putExtra"); params(Type.String, Type.String) }
        argument(1) { string(EXTRA_TEXT) }
    }
