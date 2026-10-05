// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.*
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.*
import app.reseam.patches.youtube.interaction.seekbarOnDraw
import app.reseam.patches.youtube.internal.*

val sponsorBlock = patch("SponsorBlock") {
    description("Skips crowdsourced segments, marks the seekbar, and supports segment voting and submissions.")
    compatibleWith(YOUTUBE)
    dependsOn(videoInformationHook, playerTypeHook, playerControls)
    settings(youTubeSettings,
        section(YouTubeSettingsPages.Video, "SponsorBlock", YouTubeSettings.sbEnabled,
            YouTubeSettings.sbSponsor, YouTubeSettings.sbSelfpromo, YouTubeSettings.sbInteraction,
            YouTubeSettings.sbPoiHighlight, YouTubeSettings.sbIntro, YouTubeSettings.sbOutro,
            YouTubeSettings.sbPreview, YouTubeSettings.sbHook, YouTubeSettings.sbFiller, YouTubeSettings.sbMusicOfftopic,
            YouTubeSettings.sbVotingButton, YouTubeSettings.sbCreateNewSegment,
            YouTubeSettings.sbCompactSkipButton, YouTubeSettings.sbSquareLayout,
            YouTubeSettings.sbAutoHideSkipButton, YouTubeSettings.sbAutoHideSkipButtonDuration,
            YouTubeSettings.sbToastOnSkip, YouTubeSettings.sbToastOnConnectionError,
            YouTubeSettings.sbTrackSkipCount, YouTubeSettings.sbMinSegmentDuration,
            YouTubeSettings.sbVideoLengthWithoutSegments, YouTubeSettings.sbApiUrl),
        section(YouTubeSettingsPages.Appearance, "SponsorBlock colors", YouTubeSettings.sbSponsorColor,
            YouTubeSettings.sbSelfpromoColor, YouTubeSettings.sbInteractionColor, YouTubeSettings.sbPoiHighlightColor,
            YouTubeSettings.sbIntroColor, YouTubeSettings.sbOutroColor, YouTubeSettings.sbPreviewColor,
            YouTubeSettings.sbHookColor, YouTubeSettings.sbFillerColor, YouTubeSettings.sbMusicOfftopicColor))

    execute {
        hookVideoId(SponsorBlock.newVideoLoaded)
        hookBackgroundPlayVideoId(SponsorBlock.newVideoLoaded)
        onCreateHook(SponsorBlock.initialize)
        videoTimeHook(SponsorBlock.setVideoTime)
        mainActivityOnCreate.after { call(SponsorBlock.attach, thisObject) }
        nativePlayerTypeSetter.before { call(SponsorBlock.playerView, thisObject) }

        // onDraw sets this Rect to the bar YouTube draws in this frame. Its height follows the
        // controls' fade, so it is empty while the seekbar is hidden.
        val bar = seekbarOnDraw.points("seekbar bar bounds") {
            invokeVirtual { owner("android.graphics.Rect"); name("set"); params(Type.Int, Type.Int, Type.Int, Type.Int) }
        }.all.first().writer(0).field()
        val rect = seekbarOnDraw.reserveLocal("sponsorBlockBar", "android.graphics.Rect")
        seekbarOnDraw.before { local(rect).assign(thisObject.field(bar)) }
        val thumb = seekbarOnDraw.points("seekbar circles") {
            invokeVirtual { owner("android.graphics.Canvas"); name("drawCircle")
                params(Type.Float, Type.Float, Type.Float, "android.graphics.Paint") }
        }.all.last()
        thumb.captureArgumentAs("canvas", 0, "android.graphics.Canvas").before {
            call(SponsorBlock.drawSegments, capture("canvas"), local(rect))
        }

        val totalTime = resources.id("string", "total_time")?.toLong() ?: error("string/total_time is missing")
        method("player total time text") {
            literals(totalTime)
            params("java.lang.CharSequence", "java.lang.CharSequence", "java.lang.CharSequence")
            returns(Type.Void)
        }.point("formatted total time") {
            invokeVirtual { owner("android.content.res.Resources"); name("getString")
                params(Type.Int, "[Ljava/lang/Object;"); returns(Type.String) }
            argument(1) { literal(totalTime) }
        }.next { resultOf(Type.String) }.captureAs("time", Type.String).after {
            capture("time").assign(call(SponsorBlock.appendTime, capture("time")))
        }

        hookAdVisibility(SponsorBlock.setAdVisibility)
    }
}

private object SponsorBlock : ExtClass("app.reseam.youtube.sponsorblock.SponsorBlock") {
    val attach by static(Type.Activity)
    val playerView by static(Type.View)
    val initialize by static("app.reseam.youtube.video.VideoInformation\$PlaybackController")
    val newVideoLoaded by static(Type.String)
    val setVideoTime by static(Type.Long)
    val setAdVisibility by static(Type.Int)
    val drawSegments by static("android.graphics.Canvas", "android.graphics.Rect")
    val appendTime by static(Type.String, returns = Type.String)
}
