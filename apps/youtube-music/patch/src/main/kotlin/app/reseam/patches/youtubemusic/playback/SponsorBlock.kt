// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.playback

import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.ChoiceSetting
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.playerConstructor
import app.reseam.patches.youtubecommon.playerParameterBuilderVideoClass
import app.reseam.patches.youtubecommon.playerSeekMethod
import app.reseam.patches.youtubecommon.playerSeekSource
import app.reseam.patches.youtubecommon.playerTimeMethod
import app.reseam.patches.youtubecommon.videoIdGetter
import app.reseam.patches.youtubemusic.YouTubeMusicPatchStrings
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubecommon.settings.SponsorBlockSettings
import app.reseam.youtubecommon.settings.YouTubeCommonStrings
import app.reseam.youtubemusic.sponsorblock.bindings.SponsorBlock

val sponsorBlock = patch(YouTubeMusicPatchStrings.sponsorBlock) {
    description(YouTubeMusicPatchStrings.sponsorBlockDescription)
    compatibleWith(YOUTUBE_MUSIC)
    settings(
        youTubeMusicSettings,
        section(
            YouTubeMusicSettingsPages.Playback,
            YouTubeCommonStrings.sectionSponsorblock,
            SponsorBlockSettings.sbEnabled,
            SponsorBlockSettings.sbSponsor.automatic(),
            SponsorBlockSettings.sbSelfpromo.automatic(),
            SponsorBlockSettings.sbInteraction.automatic(),
            SponsorBlockSettings.sbIntro.automatic(),
            SponsorBlockSettings.sbOutro.automatic(),
            SponsorBlockSettings.sbPreview.automatic(),
            SponsorBlockSettings.sbHook.automatic(),
            SponsorBlockSettings.sbFiller.automatic(),
            SponsorBlockSettings.sbMusicOfftopic.automatic(),
            SponsorBlockSettings.sbToastOnSkip,
            SponsorBlockSettings.sbToastOnConnectionError,
            SponsorBlockSettings.sbTrackSkipCount,
            SponsorBlockSettings.sbMinSegmentDuration,
            SponsorBlockSettings.sbApiUrl,
        ),
    )

    execute {
        SponsorBlock.seekTo.implement {
            val player = param(0).cast(playerParameterBuilderVideoClass.descriptor)
            returnValue(player.call(playerSeekMethod, param(1), staticField(playerSeekSource)))
        }
        // Outside the gate, so turning SponsorBlock on does not wait for the next player.
        playerConstructor.after { SponsorBlock.setPlayer(thisObject) }
        gate(SponsorBlockSettings.sbEnabled) {
            // The first response and nonce are the video's, the second ones an ad's.
            videoStageEventConstructor.before {
                whenNotNull(param(1)) { SponsorBlock.setVideo(param(1).call(videoIdGetter), param(4)) }
            }
            playerTimeMethod.before { SponsorBlock.setVideoTime(param(0), param(2), param(3), param(8)) }
            mediaSessionState.before { SponsorBlock.setPlaybackState(capture("playbackState")) }
        }
    }
}

// YouTube Music has no skip button or seekbar marks. It offers the behaviors that work without them,
// and shows a default that needs them as Ignore, which is what the extension does with it.
private fun ChoiceSetting.automatic() =
    copy(
        default = default.takeIf { it == "skip" || it == "skip-once" } ?: "ignore",
        choices = choices.filter { it.value == "skip" || it.value == "skip-once" || it.value == "ignore" },
    )

// The player broadcasts this event at each stage of a video. The video-ID hook YouTube uses reads it
// on the watch screen, which is gone while only the playback service runs, such as after the app is
// closed or on Android Auto.
private val videoStageEvent = klass("videoStageEvent") { strings("VideoStageEvent{stage=") }

private val videoStageEventConstructor = videoStageEvent.methods("videoStageEventConstructor") {
    flags(AccessFlags.CONSTRUCTOR)
    custom { !isStatic }
}.single { true }

// The media session wrapper hands every playback state on to the framework session here.
private val mediaSessionState = method("mediaSessionStateSetter") {
    calls { owner("android.media.session.MediaSession"); name("setPlaybackState") }
}.point { invokeVirtual { owner("android.media.session.MediaSession"); name("setPlaybackState") } }
    .captureArgumentAs("playbackState", 1, "android.media.session.PlaybackState")
