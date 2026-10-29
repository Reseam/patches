// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.images

import app.reseam.patch.after
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.pinterestSettings
import app.reseam.patches.pinterest.refs.requestOriginalSize
import app.reseam.patches.pinterest.refs.ORIGINAL_SIZE
import app.reseam.patches.pinterest.refs.closeupImageSizes
import app.reseam.patches.pinterest.refs.requestedImageSizes
import app.reseam.pinterest.settings.ImageSettings
import app.reseam.pinterest.settings.PinterestStrings

val originalQuality = patch("Original-quality images") {
    description("Loads images in original quality.")
    compatibleWith(PINTEREST)
    settings(pinterestSettings, section(PinterestStrings.sectionImages, ImageSettings.originalQuality))

    execute {
        gate(ImageSettings.originalQuality) {
            requestedImageSizes.after { requestOriginalSize() }
            closeupImageSizes.forEach { after { returnValue(string(ORIGINAL_SIZE)) } }
        }
    }
}
