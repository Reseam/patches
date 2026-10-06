// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.patch
import app.reseam.patches.youtubecommon.lithoColorHook
import app.reseam.patches.youtubecommon.registerLithoColorHook
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC

val theme = patch("Theme") {
    description("Changes YouTube Music's black background to a color of your choice.")
    compatibleWith(YOUTUBE_MUSIC)
    dependsOn(lithoColorHook)
    val backgroundColor = stringOption(
        "backgroundColor",
        title = "Background color",
        description = "The color that replaces YouTube Music's black background, as #AARRGGBB.",
        default = "#FF000000",
        required = true,
    )

    execute {
        val color = options[backgroundColor]
        val argb = java.lang.Long.parseLong(color.removePrefix("#"), 16).toInt()
        // yt_black_pure and ytm_color_black, the backgrounds of the app's themes, resolve to it.
        resources.addColor("yt_ref_color_constants_baseline_black_black_pure", color)
        MusicTheme.background.implement { returnValue(int(argb)) }
        registerLithoColorHook(MusicTheme.paintColor)
    }
}

private object MusicTheme : ExtClass("app.reseam.youtubemusic.layout.MusicTheme") {
    val background by static(returns = Type.Int)
    val paintColor by static(Type.Int, returns = Type.Int)
}
