// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.refs

import app.reseam.patch.patch
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.MediaRefs
import app.reseam.patches.instagram.core.signatureCheck
import app.reseam.patches.instagram.internal.InstagramMediaGraph

val mediaRefs = patch("Media refs") {
    description("Internal: binds app.reseam.instagram.refs.Media bridges to Instagram's media value class.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    enabledByDefault(false)

    execute {
        val media = InstagramMediaGraph.media
        MediaRefs.photoUrl.implement { returnValue(media.member("imageUrl", param(0))) }
        MediaRefs.videoUrl.implement { returnValue(media.member("videoUrl", param(0))) }
        MediaRefs.children.implement { returnValue(media.member("carouselChildren", param(0))) }
    }
}
