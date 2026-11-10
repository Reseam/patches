// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.refs

import app.reseam.instagram.refs.bindings.Media
import app.reseam.patch.patch
import app.reseam.patches.instagram.core.EXTENDED_IMAGE_URL
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.signatureCheck
import app.reseam.patches.instagram.internal.InstagramMediaGraph

val mediaRefs = patch {
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)

    execute {
        val media = InstagramMediaGraph.media
        Media.imageCandidates.implement {
            returnValue(InstagramMediaGraph.imageInfo.member("candidates", param(0).cast("com.instagram.feed.media.Media").call(InstagramMediaGraph.imageInfoGetter)))
        }
        Media.imageCandidateUrl.implement {
            returnValue(param(0).cast(EXTENDED_IMAGE_URL).callVirtual(EXTENDED_IMAGE_URL, "getUrl", "()Ljava/lang/String;"))
        }
        Media.videoUrl.implement { returnValue(media.member("videoUrl", param(0))) }
        Media.children.implement { returnValue(media.member("carouselChildren", param(0))) }
    }
}
