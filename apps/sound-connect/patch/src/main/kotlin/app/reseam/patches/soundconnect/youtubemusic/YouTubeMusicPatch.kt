// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.soundconnect.youtubemusic

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patches.soundconnect.core.SOUND_CONNECT
import app.reseam.soundconnect.youtubemusic.bindings.AppSpecs
import app.reseam.soundconnect.youtubemusic.bindings.YouTubeMusicPackage

private const val PLAY_STORE_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"

val reseamYouTubeMusic = patch("Reseam YouTube Music") {
    description("Uses Reseam's YouTube Music for Auto Play and linked services when it is installed, and Google's otherwise.")
    compatibleWith(SOUND_CONNECT)
    val packageName = stringOption(
        "youTubeMusicPackageName",
        title = "YouTube Music package name",
        description = "The package name Reseam's YouTube Music was patched with",
        default = "app.reseam.android.apps.youtube.music",
        required = true,
    )

    execute {
        YouTubeMusicPackage.reseamPackage.implement { returnValue(string(options[packageName])) }
        appEntry { YouTubeMusicPackage.init(application) }

        // The linked services list comes from Sony's server, and is cached as JSON that this parses again on every read.
        appSpecListParser.before { param(0).assign(AppSpecs.rewrite(param(0))) }

        // Auto Play's music apps are built in, with the package written into the code.
        val builtIn = builtInPackageNames.points { string(PLAY_STORE_YOUTUBE_MUSIC) }.all
        builtIn.forEach {
            it.captureAs("packageName", Type.String)
                .after { capture("packageName").assign(YouTubeMusicPackage.resolve(capture("packageName"))) }
        }
        log.info("Resolving YouTube Music's package at ${builtIn.size} built-in uses")
    }
}

private val appSpecListParser = method("parseAppSpecList") {
    strings("appSpecList", "pkgName")
    params(Type.String)
    returns("java.util.List")
}

private val builtInPackageNames = methods("builtInPackageNames") { strings(PLAY_STORE_YOUTUBE_MUSIC) }
