// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.media

import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.EXTENDED_IMAGE_URL
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.instagram.settings.MediaSettings
import app.reseam.patches.instagram.core.instagramSettings

val maxResolution = patch(InstagramPatchStrings.maxResolution) {
    description(InstagramPatchStrings.maxResolutionDescription)
    compatibleWith(INSTAGRAM)
    settings(instagramSettings, section(InstagramStrings.sectionMedia, MediaSettings.maxResolution, MediaSettings.originalQuality))

    execute {
        gate(MediaSettings.maxResolution) {
            imageUrlSelector.before {
                returnValue(paramOfType(Type.List)[int(0)].cast(EXTENDED_IMAGE_URL))
            }
        }
        // The server sizes image candidates to the screen in the User-Agent. Doubling the density
        // with the size keeps the reported dp width, and the layouts chosen from it, unchanged.
        gate(MediaSettings.originalQuality) {
            listOf(userAgentDisplay(CONFIGURATION, "densityDpi"), userAgentDisplay(POINT, "x"), userAgentDisplay(POINT, "y"))
                .forEach { value ->
                    value.after {
                        val reported = capture("display")
                        reported.assign(reported + reported)
                    }
                }
        }
    }
}

val imageUrlSelectorClass = klass("imageUrlSelectorClass") {
    strings("_8.jpg", "_6.jpg")
}

val imageUrlSelector = method("imageUrlSelector") {
    inClass(imageUrlSelectorClass)
    returns(EXTENDED_IMAGE_URL)
    hasParam(Type.List)
}

private const val CONFIGURATION = "android.content.res.Configuration"
private const val POINT = "android.graphics.Point"

private val userAgentBuilder = method("userAgentBuilder") { strings("%sdpi; %sx%s") }

private fun userAgentDisplay(owner: String, name: String) = userAgentBuilder
    .point { field { owner(owner); name(name) } }
    .captureAs("display", Type.Int)
