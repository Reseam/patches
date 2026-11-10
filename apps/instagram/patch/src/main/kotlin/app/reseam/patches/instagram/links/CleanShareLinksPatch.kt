// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.links

import app.reseam.instagram.links.bindings.ShareLinks
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.instagram.settings.LinkSettings
import app.reseam.patch.ClassTarget
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val cleanShareLinks = patch(InstagramPatchStrings.cleanShareLinks) {
    description(InstagramPatchStrings.cleanShareLinksDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionLinks, LinkSettings.cleanShareLinks))

    execute {
        // Copy link, Share and the app targets all take the link from the server's share-url response.
        val responses = mapOf(
            "mediaUrl" to parsedResponse("permalink", "XDTPermalinkResponse"),
            "profileUrl" to parsedResponse("profile_to_share_url", "XDTProfileThirdPartySharingUrlResponse"),
            "storyUrl" to klass("com.instagram.api.schemas.StoryItemThirdPartySharingUrlResponseImpl"),
            "liveUrl" to klass("com.instagram.api.schemas.LiveThirdPartySharingUrlResponseImpl"),
            "locationUrl" to klass("com.instagram.api.schemas.LocationThirdPartySharingUrlResponseImpl"),
        )
        gate(LinkSettings.cleanShareLinks) {
            responses.forEach { (name, response) ->
                method(name) {
                    inClass(response)
                    returns(Type.String)
                    paramCount(0)
                }.after { returnValue(ShareLinks.clean(result)) }
            }
        }
    }
}

/** The response class whose parser stores the [key] value right after naming the [typename]. */
private fun parsedResponse(key: String, typename: String): ClassTarget {
    val url = method("$key parser") {
        name("unsafeParseFromJson")
        strings(key, typename)
    }
        .point { string(typename) }
        .next { opcode(Opcode.IPUT_OBJECT) }
        .field("$key field")
    return klass(url.owner)
}
