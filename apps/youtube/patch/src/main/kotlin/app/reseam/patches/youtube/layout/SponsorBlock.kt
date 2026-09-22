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

        // onLayout first copies the full track bounds before applying the horizontal insets.
        // Follow that receiver to its field; the class has several other Rects for progress.
        val bounds = klass(seekbarOnDraw.owner).method("onLayout") {
            params(Type.Boolean, Type.Int, Type.Int, Type.Int, Type.Int)
        }.point("seekbar track layout") {
            invokeVirtual { owner("android.graphics.Rect"); name("set"); params("android.graphics.Rect") }
        }.writer(0).field()
        val rect = seekbarOnDraw.reserveLocal("sponsorBlockBounds", "android.graphics.Rect")
        seekbarOnDraw.before { local(rect).assign(thisObject.field(bounds)) }
        val thumb = seekbarOnDraw.points("seekbar circles") {
            invokeVirtual { owner("android.graphics.Canvas"); name("drawCircle")
                params(Type.Float, Type.Float, Type.Float, "android.graphics.Paint") }
        }.all.last()
        thumb.captureArgumentAs("canvas", 0, "android.graphics.Canvas")
            .captureArgumentAs("centerY", 2, Type.Float)
            .captureArgumentAs("radius", 3, Type.Float).before {
                call(SponsorBlock.drawSegments, capture("canvas"), local(rect), capture("centerY"), capture("radius"))
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
    val attach = static("attach", Type.Activity)
    val playerView = static("playerView", Type.View)
    val initialize = static("initialize", "app.reseam.youtube.video.VideoInformation\$PlaybackController")
    val newVideoLoaded = static("newVideoLoaded", Type.String)
    val setVideoTime = static("setVideoTime", Type.Long)
    val setAdVisibility = static("setAdVisibility", Type.Int)
    val drawSegments = static("drawSegments", "android.graphics.Canvas", "android.graphics.Rect", Type.Float, Type.Float)
    val appendTime = static("appendTime", Type.String, returns = Type.String)
}
