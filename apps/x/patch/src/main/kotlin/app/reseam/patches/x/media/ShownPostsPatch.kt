// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.media

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.ref
import app.reseam.patch.field
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patches.x.core.FUNCTION1
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.availablePost
import app.reseam.patches.x.core.postHeader
import app.reseam.patches.x.core.postOptionsState
import app.reseam.patches.x.core.xPosts
import app.reseam.x.download.bindings.ShownPosts
import app.reseam.x.download.bindings.XMedia

// Post headers report the post they draw, so a save knows which post its media came from.
val shownPosts = patch {
    compatibleWith(X)
    dependsOn(xPosts)

    execute {
        val (optionsButton, postHeaders) = postHeader()
        ShownPosts.optionsKey.implement { returnValue(param(0).cast(postOptionsState.descriptor).fieldOfType(FUNCTION1)) }
        val options = optionsButton.parameterTypes.indexOf(postOptionsState.descriptor)
        postHeaders.points { calls(optionsButton) }.forEach {
            captureArgumentAs("options", options).before { ShownPosts.track(capture("options"), paramOfType(availablePost.descriptor)) }
        }

        XMedia.isGif.implement {
            whenInstanceOf(param(0), gif.descriptor) { returnValue(bool(true)) }
            returnValue(bool(false))
        }
        XMedia.bestVariant.implement {
            for (animated in listOf(gif, video)) {
                whenInstanceOf(param(0), animated.descriptor) {
                    returnValue(call(bestVariantOf, param(0).cast(animated.descriptor).fieldOfType(immutableList)))
                }
            }
            returnNull()
        }
        XMedia.variantUrl.implement { returnValue(param(0).cast(variant.descriptor).field(variantUrl)) }
        XMedia.variantType.implement { returnValue(param(0).cast(variant.descriptor).field(variantType)) }
    }
}

private val immutableList get() = bestVariantOf.parameterTypes.single()

private val gif = klass("mediaGif") { strings("MediaContentGif(mediaId=") }
private val video = klass("mediaVideo") { strings("MediaContentVideo(mediaId=") }
private val variant = klass("mediaVariant") { strings("MediaVariant(url=") }

private val bestVariantOf = method("bestMediaVariant") {
    strings("x-mpegURL")
    returns(variant.descriptor)
    custom { parameterTypes.size == 1 && parameterTypes[0].startsWith("Lkotlinx/collections/immutable/") }
}
private val variantType = bestVariantOf.field("mediaVariantType") { owner(variant.descriptor); type(Type.String) }
private val variantUrl = fieldTarget("mediaVariantUrl") {
    variant.classDef.instanceFields.single { it.fieldType == Type.String && it.name != variantType.name }.ref
}
