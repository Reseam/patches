// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.media

import app.reseam.patch.CodeScope
import app.reseam.patch.MethodTarget
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.dex.ref
import app.reseam.patch.field
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.reserveLocal
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.COMPOSER
import app.reseam.patches.x.core.MODIFIER
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xIcon
import app.reseam.patches.x.core.unitInstance
import app.reseam.patches.x.core.xSettings
import app.reseam.x.download.bindings.DownloadButton
import app.reseam.x.postheader.bindings.DownloadClick
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

private const val FUNCTION0 = "Lkotlin/jvm/functions/Function0;"
private const val FUNCTION1 = "Lkotlin/jvm/functions/Function1;"

// Compose's defaults mask for padding(start, top, end, bottom): top and bottom left at 0.
private const val PADDING_TOP_BOTTOM_DEFAULT = 0b1010

val downloadButton = patch("Download button") {
    description("Adds a download button next to the post menu on posts with photos, GIFs or videos.")
    compatibleWith(X)
    dependsOn(saveMedia)
    settings(xSettings, section(XStrings.sectionMedia, XSettings.downloadButton))

    execute {
        val optionsLabel = resources.id("string", "button_action_options_tweet") ?: error("string/button_action_options_tweet missing")
        val downloadIcon = resources.id("drawable", "ic_vector_arrow_inbox")?.toInt() ?: error("drawable/ic_vector_arrow_inbox missing")
        val optionsButton = method("postOptionsButton") {
            literals(optionsLabel.toLong())
            param(0, optionsState.descriptor)
        }
        val postHeaders = methods("postHeaders") {
            param(0, timelinePost.descriptor)
            calls(optionsButton)
        }
        val clickable = optionsButton.point { invokeStatic { hasParam(FUNCTION0); returns(MODIFIER) } }.callee("clickable")
        val drawIcon = optionsButton.point { invokeStatic { hasParam(xIcon.descriptor); hasParam(COMPOSER) } }.callee("drawIcon")
        implementMediaAccess()
        DownloadClick.invoke.implement {
            DownloadButton.saveAll(thisObject.fieldOfType(Type.Object))
            returnValue(staticField(unitInstance))
        }
        appEntry { DownloadButton.initButton(application) }

        val options = optionsButton.parameterTypes.indexOf(optionsState.descriptor)
        postHeaders.points { calls(optionsButton) }.forEach {
            captureArgumentAs("options", options).before { DownloadButton.trackOptions(capture("options"), paramOfType(timelinePost.descriptor)) }
        }

        // The download button repeats the menu button's size, clickable and icon calls with its own
        // size, click, icon and tint, so it sits in the same header row with the same placement.
        val downloadMedia = optionsButton.reserveLocal("downloadMedia", Type.Object)
        val downloadModifier = optionsButton.reserveLocal("downloadModifier", MODIFIER)
        val downloadTint = optionsButton.reserveLocal("downloadTint", Type.Long)
        val padding = postHeaders.points { calls(optionsButton) }.all.first()
            .previous { invokeStatic { params(MODIFIER, Type.Float, Type.Float, Type.Float, Type.Float, Type.Int); returns(MODIFIER) } }
            .callee("padding")
        val size = optionsButton.point { invokeStatic { params(MODIFIER, Type.Float); returns(MODIFIER) } }.callee("size")
        optionsButton.point { calls(size) }.captureArgumentAs("base", 0).before {
            local(downloadMedia).assign(DownloadButton.downloadTarget(paramOfType(optionsState.descriptor)))
            whenNotNull(local(downloadMedia)) {
                // Spacing outside the clickable keeps the gaps to its neighbours untappable.
                val spacing = DownloadButton.iconSpacing()
                val spaced = call(padding, capture("base"), spacing, spacing, spacing, spacing, int(PADDING_TOP_BOTTOM_DEFAULT))
                local(downloadModifier).assign(call(size, spaced, DownloadButton.iconSize()))
            }
        }
        // The menu icon is the secondary text color at half alpha; the download icon takes it at full.
        optionsButton.point { invokeStatic { params(Type.Long, Type.Float); returns(Type.Long) } }
            .captureArgumentAs("color", 0)
            .before { local(downloadTint).assign(capture("color")) }
        optionsButton.point { calls(clickable) }.capturingArguments(clickable).before {
            whenNotNull(local(downloadMedia)) {
                val click = newInstance(DownloadClick.descriptor, "(${Type.Object})V", local(downloadMedia))
                val args = arguments(clickable)
                    .withType(clickable, FUNCTION0, click)
                    .withType(clickable, MODIFIER, local(downloadModifier))
                local(downloadModifier).assign(call(clickable, *args.toTypedArray()))
            }
        }
        optionsButton.point { calls(drawIcon) }.capturingArguments(drawIcon).before {
            whenNotNull(local(downloadModifier)) {
                val types = drawIcon.parameterTypes
                val args = arguments(drawIcon)
                    .withType(drawIcon, xIcon.descriptor, newInstance(xIcon.descriptor, "(IZ)V", int(downloadIcon), bool(false)))
                    .withType(drawIcon, Type.String, DownloadButton.downloadLabel())
                    .withType(drawIcon, MODIFIER, local(downloadModifier))
                    .withType(drawIcon, Type.Long, local(downloadTint))
                    .toMutableList()
                // The changed flags follow the composer; none of the menu icon's stability carries over.
                args[types.indexOf(COMPOSER) + 1] = int(0)
                call(drawIcon, *args.toTypedArray())
            }
        }
    }
}

private fun PointTarget.capturingArguments(callee: MethodTarget) =
    callee.parameterTypes.indices.fold(this) { point, i -> point.captureArgumentAs("$i", i) }

private fun CodeScope.arguments(callee: MethodTarget) = callee.parameterTypes.indices.map { capture("$it") }

private fun <T> List<T>.withType(callee: MethodTarget, type: String, value: T) =
    mapIndexed { i, arg -> if (callee.parameterTypes[i] == type) value else arg }

private fun implementMediaAccess() {
    DownloadButton.postMedia.implement {
        val media = param(0).cast(timelinePost.descriptor).field(postMediaState)
        whenInstanceOf(media, regularMedia.descriptor) { returnValue(media.cast(regularMedia.descriptor).fieldOfType(immutableList)) }
        returnNull()
    }
    DownloadButton.optionsKey.implement { returnValue(param(0).cast(optionsState.descriptor).fieldOfType(FUNCTION1)) }
    DownloadButton.photoUrl.implement {
        whenInstanceOf(param(0), photo.descriptor) { returnValue(call(photo4kUrl, param(0).cast(photo.descriptor))) }
        returnNull()
    }
    DownloadButton.bestVariant.implement {
        for (animated in listOf(gif, video)) {
            whenInstanceOf(param(0), animated.descriptor) {
                returnValue(call(bestVariantOf, param(0).cast(animated.descriptor).fieldOfType(immutableList)))
            }
        }
        returnNull()
    }
    DownloadButton.variantUrl.implement { returnValue(param(0).cast(variant.descriptor).field(variantUrl)) }
    DownloadButton.variantType.implement { returnValue(param(0).cast(variant.descriptor).field(variantType)) }
}

private val optionsState = klass("postOptionsState") { strings("PostOptionsState(showOptionsDialog=") }
private val timelinePost = klass("availablePost") { strings("AvailablePost(entryId=") }
private val regularMedia = klass("regularPostMedia") { strings("RegularMedia(media=") }
private val postMediaState = fieldTarget("postMediaState") {
    timelinePost.classDef.instanceFields.single { it.fieldType == regularMedia.classDef.interfaces.single() }.ref
}
private val immutableList get() = bestVariantOf.parameterTypes.single()

private val photo = klass("mediaPhoto") { strings("MediaContentImage(mediaId=") }
private val gif = klass("mediaGif") { strings("MediaContentGif(mediaId=") }
private val video = klass("mediaVideo") { strings("MediaContentVideo(mediaId=") }
private val variant = klass("mediaVariant") { strings("MediaVariant(url=") }

private val photo4kUrl = method("photo4kUrl") {
    strings("https://pbs.twimg.com/media/", "4096x4096")
    param(0, photo.descriptor)
}

private val bestVariantOf = method("bestMediaVariant") {
    strings("x-mpegURL")
    returns(variant.descriptor)
    custom { parameterTypes.size == 1 && parameterTypes[0].startsWith("Lkotlinx/collections/immutable/") }
}
private val variantType = bestVariantOf.field("mediaVariantType") { owner(variant.descriptor); type(Type.String) }
private val variantUrl = fieldTarget("mediaVariantUrl") {
    variant.classDef.instanceFields.single { it.fieldType == Type.String && it.name != variantType.name }.ref
}
