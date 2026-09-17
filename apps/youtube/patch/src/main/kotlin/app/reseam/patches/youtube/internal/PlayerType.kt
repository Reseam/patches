// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.youtube.core.PlayerType
import app.reseam.patches.youtube.core.ShortsPlayerState
import app.reseam.patches.youtube.core.VideoState
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.youTubeSettings

// Where the player is (minimized, maximized, fullscreen, a Short) and whether it is playing.
// Everything player-related reads it, so it is hooked once here.
val playerTypeHook = patch {
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)

    execute {
        playerTypeSetter.before { call(PlayerType.set, param(0)) }

        // Resource ids only exist against a loaded app, so the targets they seed are built here.
        val reelWatchPlayer = resources.id("id", "reel_watch_player")?.toLong()
            ?: error("id/reel_watch_player is missing")
        method("shortsPlayerInflation") {
            literals(reelWatchPlayer)
            returns(Type.View)
        }.point { literal(reelWatchPlayer) }
            .next { opcode(Opcode.MOVE_RESULT_OBJECT) }
            .captureAs("shortsPlayer", Type.View)
            .after { call(ShortsPlayerState.attach, capture("shortsPlayer")) }

        val play = resources.id("string", "accessibility_play")?.toLong() ?: error("string/accessibility_play is missing")
        val pause = resources.id("string", "accessibility_pause")?.toLong() ?: error("string/accessibility_pause is missing")
        // The play/pause button swaps its content description from the state it is handed.
        method("videoStateSetter") {
            literals(play, pause)
            params(controlsStateToString.owner)
            returns(Type.Void)
        }.before {
            // The app itself tolerates a null state here, so reading the field cannot be unconditional.
            whenNotNull(param(0)) {
                call(VideoState.set, param(0).field(videoStateField))
            }
        }
    }
}

val playerTypeEnum = klass("playerTypeEnum") {
    strings("WATCH_WHILE_PICTURE_IN_PICTURE")
}

val playerOverlaysLayout = klass("com.google.android.apps.youtube.app.common.player.overlay.YouTubePlayerOverlaysLayout")

val playerTypeSetter = method("playerTypeSetter") {
    inClass(playerOverlaysLayout)
    params(playerTypeEnum.descriptor)
    returns(Type.Void)
    // A private overload takes the same enum.
    flags(AccessFlags.PUBLIC)
}

// The controls state holds the video state and a buffering flag; only its toString names both.
val controlsStateToString = method("controlsStateToString") {
    strings("videoState", "isBuffering")
    returns(Type.String)
    params()
}

// toString labels each field as it reads it, which is the one place the video state field is named.
val videoStateField = controlsStateToString
    .point { string("videoState") }
    .next { opcode(Opcode.IGET_OBJECT) }
    .field("videoStateField")
