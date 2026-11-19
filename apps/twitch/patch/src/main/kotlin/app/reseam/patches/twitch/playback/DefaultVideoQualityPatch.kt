// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.playback

import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.field
import app.reseam.patch.function
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.TheatreQuality
import app.reseam.twitch.playback.bindings.VideoQuality
import app.reseam.twitch.settings.PlaybackSettings
import app.reseam.twitch.settings.TwitchStrings

// Native code calls the IVS player classes, so they keep their names.
private const val IVS_PLAYER = "com.amazonaws.ivs.player.Player"
private const val IVS_PLAYER_STATE = $$"com.amazonaws.ivs.player.Player$State"
private const val IVS_MEDIA_PLAYER = "Lcom/amazonaws/ivs/player/MediaPlayer;"

// Twitch's R8 rules keep the names of its models.
private const val CLIP_QUALITY = "tv.twitch.android.models.clips.ClipQuality"

val defaultVideoQuality = patch(TwitchPatchStrings.defaultVideoQuality) {
    description(TwitchPatchStrings.defaultVideoQualityDescription)
    compatibleWith(TWITCH)
    settings(
        twitchSettings,
        section(
            TwitchSettingsPages.Video,
            TwitchStrings.sectionVideoQuality,
            PlaybackSettings.defaultVideoQuality,
            PlaybackSettings.mobileDataQuality,
            PlaybackSettings.rememberVideoQuality,
        ),
    )

    execute {
        appEntry { VideoQuality.init(application) }

        // Twitch passes its default to the lookup of the saved choice, so a quality picked in the player still wins.
        // With Remember video quality off, the lookup uses a suffix Twitch never saves under, so it returns that default.
        val verticalReads = verticalTheatreQualityReads.all
        require(verticalReads.isNotEmpty()) { "The vertical theatre no longer reads the saved quality" }
        val startQualities = verticalReads + theatreQualityRead
        startQualities.forEach { read ->
            read.captureArgumentAs("suffix", 1).captureArgumentAs("default", 2).before {
                capture("default").assign(VideoQuality.initial(capture("default")))
                capture("suffix").assign(VideoQuality.savedQualitySuffix(capture("suffix")))
            }
        }

        // The player lists the stream's renditions once it is ready, which is when Twitch applies the quality it asked for before loading.
        val renditionPicks = pendingQualityApplies.all
        require(renditionPicks.isNotEmpty()) { "The player no longer applies a pending quality" }
        renditionPicks.forEach { pick ->
            pick.captureArgumentAs("player", 0).captureArgumentAs("quality", 1).before {
                capture("quality").assign(VideoQuality.rendition(capture("quality"), capture("player").fieldOfType(IVS_MEDIA_PLAYER)))
            }
        }

        // Live streams play in the React Native theatre, whose JavaScript applies a quality you saved only after these.
        reactNativePlayerReady.before { VideoQuality.startQuality(param(0), capture("player"), param(0).field(ownsAdState)) }
        reactNativeHandoff.before { VideoQuality.startQuality(thisObject, param(0), thisObject.field(ownsAdState)) }

        // Twitch's JavaScript saves a pick of Auto as no choice, so the extension remembers it.
        autoQualityPick.before { VideoQuality.rememberPick(bool(true), thisObject.field(ownsAdState)) }
        qualityPick.before { VideoQuality.rememberPick(bool(false), thisObject.field(ownsAdState)) }

        // With Remember video quality off, the theatre's player never reads as ready for the quality its JavaScript saved.
        preferredVideoQuality.before {
            whenFalse(valueOf(PlaybackSettings.rememberVideoQuality)) { param(0).assign(TheatreQuality.withoutSavedQuality(param(0))) }
        }

        log.info("Set the start quality at ${startQualities.size} player starts and matched it to a rendition at ${renditionPicks.size} places")
    }
}

// Reads the saved quality for live streams, videos or clips, or returns the default the caller passes in.
private val savedQuality = method("savedQuality") {
    strings("video_quality")
    params(Type.String, Type.String)
    returns(Type.String)
}

// Picks the theatre's start quality, with 480p as the default for clips and Auto for everything else.
private val theatreQualityRead = method("theatreQuality") {
    calls(savedQuality)
    accesses { owner(CLIP_QUALITY); name("Quality480p") }
}
    .point("theatreQualityRead") { calls(savedQuality) }

// The vertical theatre swaps a saved audio-only choice for the last video quality.
private val verticalTheatreQualityReads = methods("verticalTheatreQualities") {
    calls(savedQuality)
    strings("audio_only")
}
    .points("verticalTheatreQualityReads") { calls(savedQuality) }

private val setQualityByName = method("setQualityByName") {
    strings("auto")
    params(Type.String, Type.Boolean)
    calls { owner(IVS_PLAYER); name("setAutoQualityMode") }
}

// Applies the pending quality once the player is ready, and again when it resumes.
private val pendingQualityApplies = methods("pendingQualityAppliers") {
    calls(setQualityByName)
    calls { owner(IVS_PLAYER); name("play") }
}
    .points("pendingQualityApplies") { calls(setQualityByName) }

// The React Native player library keeps its names.
private val reactNativePlayback = klass("com.twitchrn.player.PlaybackSlot")

// The theatre and other main players own the ad state, while feed previews keep Twitch's Auto, capped to their size.
private val ownsAdState = reactNativePlayback.field("ownsAdState")

// The ready handler sends the stream's renditions to JavaScript.
private val reactNativePlayerReady = method("reactNativeStateChange") {
    inClass(reactNativePlayback)
    calls { name("emitPlaybackState") }
    accesses { owner(IVS_PLAYER_STATE); name("READY") }
}
    .point("readyPlayer") { invokeDirect { name("getPlayer") } }
    .next { resultOf() }
    .captureAs("player")
    .next { invokeVirtual { name("emitQualities") } }

// A player that a feed preview hands over keeps playing, so it skips the ready handler.
private val reactNativeHandoff = reactNativePlayback
    .method("replayHandedOverState")
    .point("handoffQualities") { invokeVirtual { name("emitQualities") } }

private val autoQualityPick = reactNativePlayback.method("setAutoQuality")
private val qualityPick = reactNativePlayback.method("setQuality")

// Applies the quality the React Native theatre saved, once its player is ready.
private val preferredVideoQuality = function("preferredVideoQuality") {
    name("usePreferredVideoQuality")
    strings("preferredVideoQuality", "isPlayerReady")
    paramCount(1)
}
