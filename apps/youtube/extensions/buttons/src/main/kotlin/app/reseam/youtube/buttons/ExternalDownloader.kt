// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("ExternalDownloader")

package app.reseam.youtube.buttons

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import app.reseam.bundle.Logger
import app.reseam.bundle.showToast
import app.reseam.runtime.Export
import app.reseam.youtube.controls.playerControlButton
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.debug
import app.reseam.youtube.core.findActivity
import app.reseam.youtube.settings.DownloadSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.video.VideoInformation

private enum class Downloader(
    val appName: () -> String,
    val packageName: String,
    val downloadUrl: String,
) {
    YTDLNIS(
        { YouTubeStrings.externalDownloaderYtdlnis },
        "com.deniscerri.ytdl",
        "https://ytdlnis.org",
    ),
    SEAL(
        { YouTubeStrings.externalDownloaderSeal },
        "com.junkfood.seal",
        "https://github.com/JunkFood02/Seal/releases/latest",
    ),
    GRAYJAY(
        { YouTubeStrings.externalDownloaderGrayjay },
        "com.futo.platformplayer",
        "https://grayjay.app",
    ),
    LIBRETUBE(
        { YouTubeStrings.externalDownloaderLibretube },
        "com.github.libretube",
        "https://libretube.dev",
    ),
    NEWPIPE(
        { YouTubeStrings.externalDownloaderNewpipe },
        "org.schabi.newpipe",
        "https://newpipe.net",
    ),
    PIPEPIPE(
        { YouTubeStrings.externalDownloaderPipepipe },
        "InfinityLoop1309.NewPipeEnhanced",
        "https://pipepipe.dev",
    ),
    TUBULAR(
        { YouTubeStrings.externalDownloaderTubular },
        "org.polymorphicshade.tubular",
        "https://github.com/polymorphicshade/Tubular/releases/latest",
    );

    fun openWebsite(activity: Activity) {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            activity.showToast(YouTubeStrings.externalDownloaderNoBrowser, Toast.LENGTH_SHORT)
        }
    }
}

@Export
@JvmName("initialize")
fun initializeExternalDownloader(root: View) {
    playerControlButton(
        root,
        "reseam_external_download_button",
        enabled = { DownloadSettings.externalDownloader.value },
    ) {
        launchExternalDownloader(VideoInformation.videoId, it.context)
    }
}

@Export
fun inAppDownloadButtonOnClick(videoId: String?): Boolean {
    if (!DownloadSettings.externalDownloaderActionButton.value) return false
    return launchExternalDownloader(videoId, YouTubeContext.mainActivity ?: YouTubeContext.applicationContext)
}

private fun launchExternalDownloader(videoId: String?, context: Context): Boolean {
    if (videoId.isNullOrEmpty()) return false
    val packageName = DownloadSettings.externalDownloaderPackageName.value.trim()
    if (packageName.isEmpty()) return false
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage(packageName)
            putExtra(Intent.EXTRA_TEXT, "https://youtu.be/$videoId")
            if (context.findActivity() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    // YouTube swallows ActivityNotFoundException; resolving first avoids its generic toast.
    if (intent.resolveActivity(context.packageManager) == null) {
        showNotInstalledDialog(context, packageName)
        return true
    }
    return try {
        context.startActivity(intent)
        true
    } catch (exception: SecurityException) {
        Logger.error(exception) { "External downloader launch denied" }
        false
    }
}

private fun showNotInstalledDialog(context: Context, packageName: String) {
    val current = context.findActivity()
    if (current == null || current.isFinishing || current.isDestroyed) {
        context.showToast(YouTubeStrings.externalDownloaderUnavailable(packageName))
        return
    }
    val downloader = Downloader.entries.firstOrNull { it.packageName == packageName }
    val dialog =
        AlertDialog.Builder(current)
            .setTitle(YouTubeStrings.externalDownloaderNotInstalledTitle)
            .setMessage(
                if (downloader != null)
                    YouTubeStrings.externalDownloaderNotInstalledMessage(downloader.appName())
                else YouTubeStrings.externalDownloaderUnknownPackage(packageName)
            )
            .setNegativeButton(android.R.string.cancel, null)
    if (downloader != null) {
        dialog.setPositiveButton(YouTubeStrings.externalDownloaderOpenWebsite) { _, _ ->
            downloader.openWebsite(current)
        }
    } else {
        dialog.setPositiveButton(android.R.string.ok, null)
    }
    dialog.show()
    Logger.debug { "External downloader not installed: $packageName" }
}
