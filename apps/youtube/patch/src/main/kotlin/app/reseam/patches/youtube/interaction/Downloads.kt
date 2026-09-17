// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.registerPlayerControlInitializer
import app.reseam.patches.youtube.internal.registerPlayerControlLayout
import app.reseam.patches.youtube.internal.videoPlaybackStateHook

object Downloads : ExtClass("app.reseam.youtube.buttons.Downloads") {
    val setMainActivity = static("setMainActivity", Type.Activity)
    val initialize = static("initialize", Type.View)
    val inAppDownloadButtonOnClick = static("inAppDownloadButtonOnClick", Type.String, returns = Type.Boolean)
}


private val offlineVideoEndpoint = method("offline video endpoint") {
    returns(Type.Void)
    paramCount(4)
    strings("Object is not an offlineable video: %s")
}

val downloads = patch("Downloads") {
    description("Adds an optional external-downloader player button and intercepts YouTube's download action.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoPlaybackStateHook, playerControls)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Downloads, "External downloader", YouTubeSettings.externalDownloader,
            YouTubeSettings.externalDownloaderActionButton, YouTubeSettings.externalDownloaderPackageName),
    )
    execute {
        resources.addId("reseam_external_download_button")
        resources.addFile("drawable", "reseam_download", "res/drawable/reseam_download.xml",
            Downloads::class.java.getResourceAsStream("/buttons/reseam_download.xml")!!.readBytes())
        registerPlayerControlLayout("/buttons/download.xml", "reseam_external_download_button", position = 2)
        registerPlayerControlInitializer(Downloads.initialize)

        mainActivityOnCreate.before { call(Downloads.setMainActivity, thisObject) }

        // This target is the four-parameter offline endpoint; parameter 2 is the video id
        // (the app's p3 register because p0 is `this`).
        offlineVideoEndpoint.before {
            whenTrue(call(Downloads.inAppDownloadButtonOnClick, param(2))) { returnVoid() }
        }
    }
}
