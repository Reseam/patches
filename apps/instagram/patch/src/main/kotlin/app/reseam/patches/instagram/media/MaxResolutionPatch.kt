// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.media

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.EXTENDED_IMAGE_URL
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.MediaSettings
import app.reseam.patches.instagram.core.instagramSettings

val maxResolution = patch("Max resolution") {
    description("Always loads the highest resolution image available.")
    compatibleWith(INSTAGRAM)
    settings(instagramSettings, section("Media", MediaSettings.maxResolution))

    execute {
        imageUrlSelector.before(MediaSettings.maxResolution) {
            val candidates = paramOfType(Type.List)
            val largest = candidates[candidates.size() - int(1)].cast(EXTENDED_IMAGE_URL)
            returnValue(largest)
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
