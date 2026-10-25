// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.shorts

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.commandRouter
import app.reseam.patches.youtube.internal.navigationBarHook
import app.reseam.youtube.settings.ShortsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.shorts.bindings.ShortsPlayer

val playShortsAsVideos = patch("Play Shorts as regular videos") {
    description("Plays Shorts in YouTube's regular video player instead of the Shorts player.")
    compatibleWith(YOUTUBE)
    dependsOn(commandRouter, navigationBarHook)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Shorts, YouTubeStrings.sectionShorts, ShortsSettings.turnOffShorts, ShortsSettings.playShortsAsVideos),
    )

    execute {
        gate(ShortsSettings.playShortsAsVideos) {
            openShort.before {
                whenTrue(ShortsPlayer.openInRegularPlayer(param(0).call(descriptorVideoId))) { returnVoid() }
            }
        }
    }
}

// Every Short opened from outside the Shorts player is placed in its fragment here.
// Some releases build the fragment descriptor in a separate, non-void factory.
private val openShort = method("openShort") {
    strings("PLAYBACK_START_DESCRIPTOR_MUTATOR", "com.google.android.apps.youtube.app.endpoint.flags")
    params(playbackStartDescriptor.descriptor, Type.Map, Type.Long, Type.String)
    returns(Type.Void)
}

private val playbackStartDescriptor = klass("playbackStartDescriptor") {
    strings("PlaybackStartDescriptor:\n  VideoId:%s\n  PlaylistId:%s\n  Index:%d\n  VideoIds:%s")
}

// The description lists the video id first, so its getter is the first text the description reads.
private val descriptorVideoId = playbackStartDescriptor.method("toString")
    .point("descriptorVideoId") {
        invokeVirtual { owner(playbackStartDescriptor.descriptor); returns(Type.String); paramCount(0) }
    }
    .callee("descriptorVideoId")
