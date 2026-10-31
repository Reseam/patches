// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.theme

import android.graphics.Color
import app.reseam.bundle.Logger
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.AppearanceSettings
import com.airbnb.lottie.LottieAnimationView
import java.io.ByteArrayInputStream

internal data class SplashColors(val primary: Int, val gradient: IntArray)

internal fun setSeekbarSplashAnimation(
    view: LottieAnimationView,
    resourceId: Int,
    colors: SplashColors?,
) {
    runCatching {
        val style = AppearanceSettings.splashScreenAnimationStyle.value
        if (colors == null || style in listOf("4", "8")) {
            view.patch_setAnimation(resourceId)
        } else {
            val json =
                YouTubeContext.applicationContext.resources.openRawResource(resourceId).use { input
                    ->
                    input.readBytes().toString(Charsets.UTF_8)
                }
            val key = "\"k\":"
            val replacement =
                json
                    .replace("${key}[1,0,0.2,1]", "$key${colorArray(colors.primary)}")
                    .replace(
                        "${key}[1,0.152941176471,0.56862745098,1]",
                        "$key${colorArray(colors.gradient[1])}",
                    )
            view.patch_setAnimation(
                ByteArrayInputStream(replacement.toByteArray(Charsets.UTF_8)),
                null,
            )
        }
    }
        .onFailure { exception ->
            Logger.debug(exception) { "setSplashAnimationLottie failure" }
            runCatching { view.patch_setAnimation(resourceId) }
        }
}

private fun colorArray(color: Int): String =
    "[${Color.red(color) / 255.0},${Color.green(color) / 255.0},${Color.blue(color) / 255.0},${Color.alpha(color) / 255.0}]"
