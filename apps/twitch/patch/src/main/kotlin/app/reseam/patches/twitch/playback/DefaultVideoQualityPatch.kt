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

        // A quality picked in the player still wins over the default.
        // Where R8 inlines Twitch's lookup, argument 1 is the whole key instead of its suffix, so the unsaved ending still gives a key with nothing saved under it.
        val verticalReads = verticalTheatreQualityReads.all
        require(verticalReads.isNotEmpty()) { "The vertical theatre no longer reads the saved quality" }
        val startQualities = verticalReads + theatreQualityRead
        startQualities.forEach { read ->
            read.captureArgumentAs("suffix", 1).captureArgumentAs("default", 2).before {
                capture("default").assign(VideoQuality.initial(capture("default")))
                capture("suffix").assign(VideoQuality.savedQualitySuffix(capture("suffix")))
            }
        }

        // Renditions are known only once the player is ready, which is when Twitch applies a pending quality.
        val renditionPicks = pendingQualityApplies.all
        require(renditionPicks.isNotEmpty()) { "The player no longer applies a pending quality" }
        renditionPicks.forEach { pick ->
            pick.captureArgumentAs("player", 0).captureArgumentAs("quality", 1).before {
                capture("quality").assign(VideoQuality.rendition(capture("quality"), capture("player").fieldOfType(IVS_MEDIA_PLAYER)))
            }
        }

        // The React Native theatre's JavaScript applies a saved quality only after these.
        reactNativePlayerReady.before { VideoQuality.startQuality(param(0), capture("player"), param(0).field(ownsAdState)) }
        reactNativeHandoff.before { VideoQuality.startQuality(thisObject, param(0), thisObject.field(ownsAdState)) }

        // Twitch's JavaScript saves a pick of Auto as no choice.
        autoQualityPick.before { VideoQuality.rememberPick(bool(true), thisObject.field(ownsAdState)) }
        qualityPick.before { VideoQuality.rememberPick(bool(false), thisObject.field(ownsAdState)) }

        preferredVideoQuality.before {
            whenFalse(valueOf(PlaybackSettings.rememberVideoQuality)) { param(0).assign(TheatreQuality.withoutSavedQuality(param(0))) }
        }

        log.info("Set the start quality at ${startQualities.size} player starts and matched it to a rendition at ${renditionPicks.size} places")
    }
}

// In each reader, the one call that takes two strings and returns one is Twitch's saved quality lookup.
// Where R8 inlines that lookup, it is the preference read the lookup made.
private val theatreQualityRead = method("theatreQuality") {
    strings("auto")
    accesses { owner(CLIP_QUALITY); name("Quality480p") }
}
    .point("theatreQualityRead") { invokeVirtual { params(Type.String, Type.String); returns(Type.String) } }

private val verticalTheatreQualityReads = methods("verticalTheatreQualities") {
    strings("audio_only")
    calls { name("getSuffix") }
    calls { params(Type.String, Type.String); returns(Type.String) }
}
    .points("verticalTheatreQualityReads") { invokeVirtual { params(Type.String, Type.String); returns(Type.String) } }

private val setQualityByName = method("setQualityByName") {
    strings("auto")
    params(Type.String, Type.Boolean)
    calls { owner(IVS_PLAYER); name("setAutoQualityMode") }
}

private val pendingQualityApplies = methods("pendingQualityAppliers") {
    calls(setQualityByName)
    calls { owner(IVS_PLAYER); name("play") }
}
    .points("pendingQualityApplies") { calls(setQualityByName) }

// The React Native player library keeps its names.
private val reactNativePlayback = klass("com.twitchrn.player.PlaybackSlot")

// Main players own the ad state; feed previews keep Twitch's Auto.
private val ownsAdState = reactNativePlayback.field("ownsAdState")

private val reactNativePlayerReady = method("reactNativeStateChange") {
    inClass(reactNativePlayback)
    calls { name("emitPlaybackState") }
    accesses { owner(IVS_PLAYER_STATE); name("READY") }
}
    .point("readyPlayer") { invokeDirect { name("getPlayer") } }
    .next { resultOf() }
    .captureAs("player")
    .next { invokeVirtual { name("emitQualities") } }

// A player handed over from a feed preview skips the ready handler.
private val reactNativeHandoff = reactNativePlayback
    .method("replayHandedOverState")
    .point("handoffQualities") { invokeVirtual { name("emitQualities") } }

private val autoQualityPick = reactNativePlayback.method("setAutoQuality")
private val qualityPick = reactNativePlayback.method("setQuality")

private val preferredVideoQuality = function {
    name("usePreferredVideoQuality")
    paramCount(1)
}
