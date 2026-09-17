// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.returnType
import app.reseam.patch.dex.methodRef
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.point
import app.reseam.patch.replace
import app.reseam.patches.youtube.core.YOUTUBE

private const val LOTTIE = "com.airbnb.lottie.LottieAnimationView"
private const val LOTTIE_FACTORY_ZIP_INPUT = "java.util.zip.ZipInputStream"
private const val LOTTIE_FACTORY_STREAM = "java.io.InputStream"

val seekbarColor = patch {
    compatibleWith(YOUTUBE)
    dependsOn(lithoColorHook)

    execute {
        registerLithoColorHook(SeekbarColorExtension.getLithoColor)

        val inlinePlayed = resources.id("color", "inline_time_bar_played_not_highlighted_color")
            ?.toLong() ?: error("color/inline_time_bar_played_not_highlighted_color is missing")
        val inlineDark = resources.id("color", "inline_time_bar_colorized_bar_played_color_dark")
            ?.toLong() ?: error("color/inline_time_bar_colorized_bar_played_color_dark is missing")
        val reelPlayed = resources.id("color", "reel_time_bar_played_color")
            ?.toLong() ?: error("color/reel_time_bar_played_color is missing")
        val liveRange = resources.id("color", "inline_time_bar_live_seekable_range")
            ?.toLong() ?: error("color/inline_time_bar_live_seekable_range is missing")
        val magenta = resources.id("color", "yt_youtube_magenta")
            ?.toLong() ?: error("color/yt_youtube_magenta is missing")

        fun hookResourceColor(method: app.reseam.patch.MethodTarget, resourceId: Long) {
            method.points("color $resourceId") {
                invoke { name("getColor"); returns(Type.Int) }
                where { methodRef?.parameterTypes?.let { it == listOf(Type.Int) || it == listOf(Type.Context, Type.Int) } == true }
                argument(1) { literal(resourceId) }
            }.all.also { check(it.isNotEmpty()) { "No color lookup for $resourceId" } }.forEach { site ->
                site.next { resultOf(Type.Int) }
                .captureAs("color", Type.Int)
                .after { capture("color").assign(call(SeekbarColorExtension.getVideoPlayerSeekbarColor, capture("color"))) }
            }
        }

        val playerColor = method("playerSeekbarColor") {
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            literals(inlinePlayed, inlineDark)
        }
        hookResourceColor(playerColor, inlinePlayed)
        hookResourceColor(playerColor, inlineDark)

        hookResourceColor(
            method("shortsSeekbarColor") {
                flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
                literals(reelPlayed)
            },
            reelPlayed,
        )

        val clickedColor = method("seekbarClickedColor") {
            strings("AD_LARGE_CONTROLS")
            opcode(Opcode.CONST_HIGH16)
        }
        // Literal queries use the runtime ARGB value, including high16 shifts.
        clickedColor.point("clickedPaintColor") { literal(-65536L) }
            .captureAs("color", Type.Int)
            .after { capture("color").assign(call(SeekbarColorExtension.getVideoPlayerSeekbarClickedColor, capture("color"))) }

        hookResourceColor(
            method("seekbarHandleColor") {
                flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
                literals(liveRange)
                custom { containsLiteral(resources.id("attr", "ytStaticBrandRed")?.toLong() ?: -1L) }
            },
            liveRange,
        )

        val watchHistory = method("watchHistoryProgress") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            returns(Type.Void)
            paramCount(1)
            literals(-1712394514L)
            calls { owner("Landroid/widget/ProgressBar;"); name("setMax") }
        }
        watchHistory.point { literal(-1712394514L) }
            .previous { opcode(Opcode.MOVE_RESULT) }
            .captureAs("progress", Type.Boolean)
            .after { capture("progress").assign(call(SeekbarColorExtension.showWatchHistoryProgressDrawable, capture("progress"))) }

        val lithoGradient = method("lithoLinearGradient") {
            flags(AccessFlags.STATIC)
            returns("android.graphics.LinearGradient")
            params(Type.Float, Type.Float, Type.Float, Type.Float, "[I", "[F")
        }
        lithoGradient.before {
            param(4).assign(call(SeekbarColorExtension.getLithoLinearGradient, param(4), param(5)))
        }

        val playerGradient = method("playerLinearGradient") {
            flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
            params(Type.Int, Type.Int, Type.Int, Type.Int, Type.Context, Type.Int)
            returns("android.graphics.LinearGradient")
            literals(magenta)
        }
        playerGradient.point("playerGradientArray") { opcode(Opcode.FILLED_NEW_ARRAY) }
            .next { opcode(Opcode.MOVE_RESULT_OBJECT) }
            .captureAs("gradient", "[I")
            .after {
                capture("gradient").assign(
                    call(SeekbarColorExtension.getPlayerLinearGradient, capture("gradient"), param(0), param(1)),
                )
            }

        val lottieClass = classTarget("lottieAnimationView") {
            bytecode.findClass(LOTTIE) ?: error("LottieAnimationView is missing")
        }
        val lottieSetAnimationInt = method("lottieSetAnimationInt") {
            inClass(lottieClass)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params(Type.Int)
            returns(Type.Void)
            calls { owner("Lcom/airbnb/lottie/LottieAnimationView;"); name("isInEditMode") }
        }
        bytecode.redirectCalls(lottieSetAnimationInt.ref, SeekbarColorExtension.setSplashAnimationLottie)

        val lottieFactoryZip = method("lottieFactoryZip") {
            flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
            params(Type.Context, LOTTIE_FACTORY_ZIP_INPUT, Type.String)
            strings("Unable to parse composition", " however it was not found in the animation.")
            custom { returnType.startsWith("L") }
        }
        val lottieFactoryClass = classTarget("lottieCompositionFactory") {
            bytecode.findClass(lottieFactoryZip.owner) ?: error("Lottie composition factory is missing")
        }
        val lottieFactoryFromJson = method("lottieFactoryFromJson") {
            inClass(lottieFactoryClass)
            flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
            params(LOTTIE_FACTORY_STREAM, Type.String)
            callsMethod { "Ljava/util/concurrent/Callable;" in parameterTypes }
        }
        val lottieSetAnimationStream = method("lottieSetAnimationStream") {
            inClass(lottieClass)
            params(lottieFactoryFromJson.returnType)
            returns(Type.Void)
            custom { name != lottieSetAnimationInt.name }
        }

        val intAlias = appHelper(lottieClass.classDef, "patch_setAnimation", "(I)V")
        intAlias.replace {
            thisObject.call(lottieSetAnimationInt, param(0))
            returnVoid()
        }
        val streamAlias = appHelper(lottieClass.classDef, "patch_setAnimation", "(Ljava/io/InputStream;Ljava/lang/String;)V")
        streamAlias.replace {
            val composition = call(lottieFactoryFromJson, param(0), param(1))
            thisObject.call(lottieSetAnimationStream, composition)
            returnVoid()
        }
    }
}

object SeekbarColorExtension : ExtClass("app.reseam.youtube.theme.SeekbarColor") {
    val setSplashAnimationLottie = static("setSplashAnimationLottie", LOTTIE, Type.Int)
    val showWatchHistoryProgressDrawable = static("showWatchHistoryProgressDrawable", Type.Boolean, returns = Type.Boolean)
    val getLithoColor = static("getLithoColor", Type.Int, returns = Type.Int)
    val getPlayerLinearGradient = static("getPlayerLinearGradient", "[I", Type.Int, Type.Int, returns = "[I")
    val getLithoLinearGradient = static("getLithoLinearGradient", "[I", "[F", returns = "[I")
    val getVideoPlayerSeekbarClickedColor = static("getVideoPlayerSeekbarClickedColor", Type.Int, returns = Type.Int)
    val getVideoPlayerSeekbarColor = static("getVideoPlayerSeekbarColor", Type.Int, returns = Type.Int)
}
