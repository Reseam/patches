// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.video

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import java.lang.ref.WeakReference

@Export
object VideoInformation {
    @Export
    interface PlaybackController {
        @Export fun patch_seekTo(videoTime: Long): Boolean

        @Export fun patch_seekToRelative(videoTimeOffset: Long)
    }

    private const val SHORTS_PLAYER_PARAMETERS = "8AEB"

    private var playerController = WeakReference<PlaybackController>(null)
    private var mdxPlayerDirector = WeakReference<PlaybackController>(null)

    var videoId = ""
        private set

    @Volatile
    var lastPlayerResponseIsShort = false
        private set

    @Volatile
    var lastVideoIdIsShort = false
        private set

    @Volatile
    var playbackSpeed = 1f
        private set

    var videoLength = 0L
        private set

    @Volatile
    var videoTime = -1L
        private set

    @Export
    @JvmStatic
    fun initialize(controller: PlaybackController) {
        playerController = WeakReference(controller)
        videoTime = -1
        videoLength = 0
        playbackSpeed = 1f
        VideoQuality.reset()
    }

    @Export
    @JvmStatic
    fun initializeMDX(director: PlaybackController) {
        mdxPlayerDirector = WeakReference(director)
    }

    @Export
    @JvmStatic
    fun setVideoId(id: String?) {
        if (id == null || videoId == id) return
        videoId = id
        Logger.debug { "Video ID: $id" }
    }

    @Export
    @JvmStatic
    fun newPlayerResponseSignature(
        signature: String?,
        responseVideoId: String?,
        isShortAndOpeningOrPlaying: Boolean,
    ): String? {
        val isShort = signature?.startsWith(SHORTS_PLAYER_PARAMETERS) == true
        lastPlayerResponseIsShort = isShort
        if ((!isShort || isShortAndOpeningOrPlaying) && lastVideoIdIsShort != isShort) {
            lastVideoIdIsShort = isShort
            Logger.debug { "Opened video is Short: $isShort" }
        }
        return signature
    }

    @Export
    @JvmStatic
    fun videoSpeedChanged(speed: Float) {
        if (playbackSpeed == speed) return
        playbackSpeed = speed
        Logger.debug { "Video speed changed: $speed" }
    }

    @Export
    @JvmStatic
    fun userSelectedPlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        Logger.debug { "User selected playback speed: $speed" }
    }

    @Export
    @JvmStatic
    fun setVideoLength(length: Long) {
        if (videoLength == length) return
        videoLength = length
        Logger.debug { "Video duration: $length" }
    }

    @Export
    @JvmStatic
    fun setVideoWindow(start: Long, end: Long) = setVideoLength((end - start).coerceAtLeast(0))

    @Export
    @JvmStatic
    fun setVideoTime(time: Long) {
        videoTime = time
    }

    @Export @JvmStatic fun overridePlaybackSpeed(speedOverride: Float) {}

    fun seekTo(seekTime: Long): Boolean {
        val adjusted =
            (if (videoLength > 0) minOf(seekTime, videoLength - 250) else seekTime).coerceAtLeast(0)
        if (videoTime in adjusted..seekTime) {
            Logger.debug { "Ignoring seek near video end: $seekTime" }
            return false
        }
        Logger.debug { "Seeking to: $adjusted" }
        return runCatching {
                playerController.get()?.patch_seekTo(adjusted) == true ||
                    (adjusted / 1000 != videoTime / 1000 &&
                        mdxPlayerDirector.get()?.patch_seekTo(adjusted) == true)
            }
            .onFailure { Logger.error(it) { "seekTo failure" } }
            .getOrDefault(false)
    }
}
