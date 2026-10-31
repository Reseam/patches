// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("CopyVideoUrl")

package app.reseam.youtube.buttons

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.Toast
import app.reseam.bundle.Logger
import app.reseam.bundle.showToast
import app.reseam.runtime.Export
import app.reseam.youtube.controls.playerControlButton
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.video.VideoInformation

private enum class Format(
    val buttonId: String,
    val enabled: () -> Boolean,
    val message: () -> String,
    val suffix: (Long) -> String,
) {
    URL(
        "reseam_copy_video_url_button",
        { PlayerControlsSettings.copyVideoUrl.value },
        { YouTubeStrings.copyVideoUrlCopied },
        { "" },
    ),
    TIMESTAMP(
        "reseam_copy_video_url_timestamp_button",
        { PlayerControlsSettings.copyVideoUrlTimestamp.value },
        { YouTubeStrings.copyVideoUrlCopiedWithTimestamp },
        { if (it > 0) "?t=$it" else "" },
    );

    val longClickFormat: Format
        get() =
            when (this) {
                URL -> TIMESTAMP
                TIMESTAMP -> URL
            }
}

@Export
@JvmName("initialize")
fun initializeCopyVideoUrl(root: View) {
    for (format in Format.entries) playerControlButton(
        root,
        format.buttonId,
        enabled = format.enabled,
        longClick = { copy(it.context, format.longClickFormat) },
    ) {
        copy(it.context, format)
    }
}

private fun copy(context: Context, format: Format) {
    try {
        val videoId = VideoInformation.videoId
        if (videoId.isEmpty()) return
        val seconds = (VideoInformation.videoTime / 1000).coerceAtLeast(0)
        val suffix = format.suffix(seconds)
        val url = "https://youtu.be/$videoId$suffix"
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager? ?: return
        clipboard.setPrimaryClip(
            ClipData.newPlainText(YouTubeStrings.copyVideoUrlClipboardLabel, url)
        )
        if (Build.VERSION.SDK_INT < 33 || suffix.isNotEmpty()) {
            context.showToast(format.message(), Toast.LENGTH_SHORT)
        }
        Logger.debug { "Copied video URL: $url" }
    } catch (exception: Exception) {
        Logger.error(exception) { "Copy video URL failure" }
    }
}
