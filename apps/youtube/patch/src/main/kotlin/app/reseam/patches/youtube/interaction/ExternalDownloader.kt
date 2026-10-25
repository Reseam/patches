// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.mainActivityHook
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.registerPlayerControlInitializer
import app.reseam.patches.youtube.internal.registerPlayerControlLayout
import app.reseam.patches.youtube.internal.videoPlaybackStateHook
import app.reseam.youtube.buttons.bindings.ExternalDownloader
import app.reseam.youtube.settings.DownloadSettings
import app.reseam.youtube.settings.YouTubeStrings

private val offlineVideoEndpoint = method("offline video endpoint") {
    returns(Type.Void)
    paramCount(4)
    param(2, Type.String)
    // Older releases concatenate the object instead of formatting the diagnostic.
    stringsStartingWith("Object is not an offlineable video: ")
}

val externalDownloader = patch("External downloader") {
    description("Sends videos to an external downloader app from a player button or YouTube's Download action.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoPlaybackStateHook, playerControls, mainActivityHook)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Downloads, YouTubeStrings.sectionExternalDownloader, DownloadSettings.externalDownloader,
            DownloadSettings.externalDownloaderActionButton, DownloadSettings.externalDownloaderPackageName),
    )
    execute {
        resources.addId("reseam_external_download_button")
        resources.addFile("drawable", "reseam_download", "res/drawable/reseam_download.xml",
            ExternalDownloader::class.java.getResourceAsStream("/buttons/reseam_download.xml")!!.readBytes())
        registerPlayerControlLayout("/buttons/download.xml", "reseam_external_download_button", position = 2)
        registerPlayerControlInitializer(ExternalDownloader.initialize)

        // This target is the four-parameter offline endpoint; parameter 2 is the video id
        // (the app's p3 register because p0 is `this`).
        offlineVideoEndpoint.before {
            whenTrue(ExternalDownloader.inAppDownloadButtonOnClick(param(2))) { returnVoid() }
        }
    }
}
