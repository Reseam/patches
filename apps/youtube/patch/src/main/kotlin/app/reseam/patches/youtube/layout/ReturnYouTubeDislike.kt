// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.*
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.literal
import app.reseam.patch.dex.opcode
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.*
import app.reseam.patches.youtube.internal.*
import app.reseam.patches.youtubecommon.*
import app.reseam.youtube.dislike.bindings.DislikeLabel
import app.reseam.youtube.dislike.bindings.ReturnYouTubeDislikeFilter
import app.reseam.youtube.dislike.bindings.DislikeRollingNumbers
import app.reseam.youtube.dislike.bindings.DislikeTextHooks
import app.reseam.youtube.dislike.bindings.DislikeVideoHooks
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings

val returnYouTubeDislike = patch("Return YouTube Dislike") {
    description("Shows dislike counts from Return YouTube Dislike and supports voting and Shorts.")
    compatibleWith(YOUTUBE)
    dependsOn(lithoFilter, lithoTextHook, videoInformationHook, playerTypeHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, YouTubeStrings.sectionReturnYoutubeDislike,
        VideoSettings.rydEnabled, VideoSettings.rydShorts, VideoSettings.rydDislikePercentage,
        VideoSettings.rydCompactLayout, VideoSettings.rydEstimatedLike, VideoSettings.rydToastOnConnectionError))

    execute {
        componentCreate.after {
            val span = DislikeLabel.create(param(1).field(conversionContextPath))
            whenNotNull(span) {
                val button = result.cast(nativeGesture.descriptor)
                val icon = button.field(nativeGesture.fieldOfType(componentCreate.returnType))
                val layout = icon.call(nativeCommonProperties).call(nativeLayoutProperties).cast(nativeHeight.owner)
                layout.set(nativeHeight, DislikeLabel.iconHeight())
                layout.set(nativeMinHeight, DislikeLabel.iconHeight())
                val heightMask = nativeHeightSetter.method.instructions.single {
                    it.opcode == Opcode.OR_INT_LIT8 || it.opcode == Opcode.OR_INT_LIT16
                }.literal!!.toInt()
                val minHeightMask = nativeMinHeightSetter.method.instructions.single {
                    it.opcode == Opcode.OR_INT_LIT8 || it.opcode == Opcode.OR_INT_LIT16
                }.literal!!.toInt()
                layout.set(nativeLayoutFlags, DislikeLabel.withFlags(layout.field(nativeLayoutFlags), int(heightMask or minHeightMask)))
                val text = newInstance(nativeText.descriptor)
                text.set(nativeTextValue, span)
                text.call(nativeCommonProperties).call(nativeWrapInView)
                val column = newInstance(nativeColumn.descriptor)
                column.set(nativeColumnAlignItems, int(3)) // Yoga CENTER, represented by the app's 1-based enum.
                column.set(nativeColumn.fieldOfType("java.util.List"),
                    DislikeLabel.children(icon, text))
                button.set(nativeGesture.fieldOfType(componentCreate.returnType), column)
            }
        }
        nativeTextAccessibility.point {
            opcode(Opcode.IGET_OBJECT)
            field { type(CHAR_SEQUENCE) }
        }.captureAs("label", CHAR_SEQUENCE).after {
            capture("label").assign(DislikeLabel.accessibilityText(capture("label")))
        }
        hookVideoId(DislikeVideoHooks.newVideoLoaded)
        hookPlayerResponseVideoId(DislikeVideoHooks.preloadVideoId)
        hookPlayerResponseVideoId(ReturnYouTubeDislikeFilter.newPlayerResponseVideoId)
        registerLithoFilter(ReturnYouTubeDislikeFilter)
        listOf("like/like" to 1, "like/dislike" to -1, "like/removelike" to 0).forEach { (endpoint, vote) ->
            method("vote $endpoint") { strings(endpoint); returns(Type.Void) }
                .before { DislikeVideoHooks.sendVote(int(vote)) }
        }

        hookLithoText(DislikeTextHooks.onLithoTextLoaded)

        val rollingContext = rollingSetter.reserveLocal("rollingPath", "java.lang.StringBuilder")
        rollingSetter.before {
            local(rollingContext).assign(nullObject)
            whenNotNull(param(1)) {
                local(rollingContext).assign(param(1).cast(conversionContext.descriptor).field(conversionContextPath))
            }
        }
        rollingSetter.point("rolling count") {
            opcode(Opcode.IGET_OBJECT)
            field { type(Type.String) }
        }.captureAs("count", Type.String).after {
            capture("count").assign(DislikeRollingNumbers.onRollingNumberLoaded(local(rollingContext), capture("count")))
        }

        // Widths are measured both statically and digit-by-digit; scope both to the font model.
        klass(rollingFontProperties.owner).methods("rolling text measurements") {
            params(Type.String)
            returns(Type.Float)
        }.forEach {
            after { returnValue(DislikeRollingNumbers.onRollingNumberMeasured(param(0), result)) }
        }

        // Stable platform call + the view superclass pins this class without a whole-DEX scan.
        klass(rollingImageUpdate.owner).methods("rolling text updates") {
            calls { name("setText"); params(CHAR_SEQUENCE) }
        }.points("rolling text assignment") {
            invokeVirtual { name("setText"); params(CHAR_SEQUENCE) }
        }.forEach {
            captureArgumentAs("view", 0, "android.widget.TextView")
                .captureArgumentAs("text", 1, CHAR_SEQUENCE).before {
                    capture("text").assign(DislikeRollingNumbers.updateRollingNumber(capture("view"), capture("text")))
                }
        }
    }
}

private val rollingSetter = method("rolling number model builder") {
    stringsStartingWith("RollingNumberType required properties missing!")
    // Older releases pass fewer context parameters to the builder.
    hasParam("com.google.protobuf.MessageLite")
}

private val rollingFontProperties = method("rolling number font properties") {
    strings("RollingNumberFontProperties{paint=")
    name("toString")
    returns(Type.String)
}

private val rollingImageUpdate = method("rolling number image update") {
    params("android.graphics.Bitmap")
    returns(Type.Void)
    calls { owner("android.text.SpannableString"); name("setSpan") }
    custom {
        classDef.info.superclass == "Landroid/support/v7/widget/AppCompatTextView;" ||
            classDef.info.superclass == "Lcom/google/android/libraries/youtube/rendering/ui/spec/typography/YouTubeAppCompatTextView;"
    }
}
