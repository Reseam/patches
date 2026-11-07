// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.*
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.*
import app.reseam.patches.youtube.interaction.seekbarOnDraw
import app.reseam.patches.youtube.internal.*
import app.reseam.youtube.settings.AppearanceSettings
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.sponsorblock.bindings.SponsorBlock
import app.reseam.youtube.sponsorblock.bindings.SponsorBlockPlayback
import app.reseam.youtube.sponsorblock.bindings.SponsorBlockSession
import app.reseam.youtubecommon.settings.SponsorBlockSettings
import app.reseam.youtubecommon.settings.YouTubeCommonStrings

val sponsorBlock = patch("SponsorBlock") {
    description("Skips crowdsourced segments, marks the seekbar, and supports segment voting and submissions.")
    compatibleWith(YOUTUBE)
    dependsOn(videoInformationHook, playerTypeHook, playerControls)
    settings(youTubeSettings,
        section(YouTubeSettingsPages.Video, YouTubeCommonStrings.sectionSponsorblock, SponsorBlockSettings.sbEnabled,
            SponsorBlockSettings.sbSponsor, SponsorBlockSettings.sbSelfpromo, SponsorBlockSettings.sbInteraction,
            SponsorBlockSettings.sbPoiHighlight, SponsorBlockSettings.sbIntro, SponsorBlockSettings.sbOutro,
            SponsorBlockSettings.sbPreview, SponsorBlockSettings.sbHook, SponsorBlockSettings.sbFiller,
            SponsorBlockSettings.sbMusicOfftopic,
            VideoSettings.sbVotingButton, VideoSettings.sbCreateNewSegment,
            VideoSettings.sbCompactSkipButton, VideoSettings.sbSquareLayout,
            VideoSettings.sbAutoHideSkipButton, VideoSettings.sbAutoHideSkipButtonDuration,
            SponsorBlockSettings.sbToastOnSkip, SponsorBlockSettings.sbToastOnConnectionError,
            SponsorBlockSettings.sbTrackSkipCount, SponsorBlockSettings.sbMinSegmentDuration,
            VideoSettings.sbVideoLengthWithoutSegments, SponsorBlockSettings.sbApiUrl),
        section(YouTubeSettingsPages.Appearance, YouTubeStrings.sectionSponsorblockColors, AppearanceSettings.sbSponsorColor,
            AppearanceSettings.sbSelfpromoColor, AppearanceSettings.sbInteractionColor, AppearanceSettings.sbPoiHighlightColor,
            AppearanceSettings.sbIntroColor, AppearanceSettings.sbOutroColor, AppearanceSettings.sbPreviewColor,
            AppearanceSettings.sbHookColor, AppearanceSettings.sbFillerColor, AppearanceSettings.sbMusicOfftopicColor))

    execute {
        resources.addId("reseam_sponsorblock_button")
        registerPlayerControlIcon("reseam_sponsorblock", "/buttons/reseam_sponsorblock.xml", "/buttons/bold/reseam_sponsorblock.xml")
        registerPlayerControlLayout("/buttons/sponsorblock.xml", "/buttons/bold/sponsorblock.xml",
            "reseam_sponsorblock_button", position = 6)
        registerPlayerControlInitializer(SponsorBlock.initializeMenuButton)

        hookVideoId(SponsorBlockSession.newVideoLoaded)
        hookBackgroundPlayVideoId(SponsorBlockSession.newVideoLoaded)
        onCreateHook(SponsorBlockPlayback.initialize)
        videoTimeHook(SponsorBlockPlayback.setVideoTime)
        mainActivityOnCreate.after { SponsorBlock.attach(thisObject) }
        nativePlayerTypeSetter.before { SponsorBlock.playerView(thisObject) }

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
            SponsorBlock.drawSegments(capture("canvas"), local(rect))
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
            capture("time").assign(SponsorBlock.appendTime(capture("time")))
        }

        hookAdVisibility(SponsorBlockPlayback.setAdVisibility)
    }
}
