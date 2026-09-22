// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.refs

import app.reseam.patch.patch
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.MediaRefs
import app.reseam.patches.instagram.core.EXTENDED_IMAGE_URL
import app.reseam.patches.instagram.core.signatureCheck
import app.reseam.patches.instagram.internal.InstagramMediaGraph

val mediaRefs = patch {
    description("Binds app.reseam.instagram.refs.Media bridges to Instagram's media value class.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)

    execute {
        val media = InstagramMediaGraph.media
        MediaRefs.imageCandidates.implement {
            returnValue(InstagramMediaGraph.imageInfo.member("candidates", param(0).cast("com.instagram.feed.media.Media").call(InstagramMediaGraph.imageInfoGetter)))
        }
        MediaRefs.imageCandidateUrl.implement {
            returnValue(param(0).cast(EXTENDED_IMAGE_URL).callVirtual(EXTENDED_IMAGE_URL, "getUrl", "()Ljava/lang/String;"))
        }
        MediaRefs.videoUrl.implement { returnValue(media.member("videoUrl", param(0))) }
        MediaRefs.children.implement { returnValue(media.member("carouselChildren", param(0))) }
    }
}
