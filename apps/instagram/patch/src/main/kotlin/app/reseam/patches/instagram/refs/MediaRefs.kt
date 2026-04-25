// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.refs

import app.reseam.patch.bindStub
import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.internal.InstagramMediaGraph

private const val REFS_MEDIA = "Lapp/reseam/instagram/refs/Media;"

val mediaRefs = patch(
    name = "Media refs",
    description = "Internal: binds app.reseam.instagram.refs.Media bridges to Instagram's media value class.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    dependsOn = listOf(signatureCheckPatch),
    enabledByDefault = false,
) {
    extendWith("instagram-refs.dex")

    execute { ctx ->
        val graph = InstagramMediaGraph(ctx)

        ctx.bindStub(REFS_MEDIA) {
            method("photoUrl", "(Ljava/lang/Object;)Ljava/lang/String;") {
                returnObject(graph.media.member("imageUrl", parameter(0)))
            }

            method("videoUrl", "(Ljava/lang/Object;)Ljava/lang/String;") {
                returnObject(graph.media.member("videoUrl", parameter(0)))
            }

            method("children", "(Ljava/lang/Object;)Ljava/util/List;") {
                returnObject(graph.media.member("carouselChildren", parameter(0)))
            }
        }

        ctx.log.info("Media refs bound through InstagramMediaGraph")
    }
}
