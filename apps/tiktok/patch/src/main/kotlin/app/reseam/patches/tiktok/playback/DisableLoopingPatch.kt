// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.playback

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.PlaybackSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val VIDEO_ENGINE = "com.ss.ttvideoengine.TTVideoEngine"
private const val EXPECTED_LOOP_CALLS = 3

val disableLooping =
    patch("Disable looping") {
        description("Videos stop at the end instead of replaying.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Playback,
                TikTokStrings.sectionPlayback,
                PlaybackSettings.disableLooping,
            ),
        )

        execute {
            val loopCalls = engineLoops.all
            check(loopCalls.size == EXPECTED_LOOP_CALLS) {
                "Expected $EXPECTED_LOOP_CALLS feed loop calls, found ${loopCalls.size}"
            }
            gate(PlaybackSettings.disableLooping) {
                loopCalls.forEach {
                    it.captureArgumentAs("loop", 1, Type.Boolean).before {
                        capture("loop").assign(bool(false))
                    }
                }
            }
        }
    }

private val playOptions =
    method("playOptions") {
        strings("is_play_loop")
        calls {
            owner(VIDEO_ENGINE)
            name("setLooping")
        }
    }

// The feed player loops by default and on every player type's request, always through these engine
// calls.
private val engineLoops =
    methods("feedPlayerLoops") {
            inClass(klass(playOptions.owner))
            calls {
                owner(VIDEO_ENGINE)
                name("setLooping")
            }
        }
        .points("engineLoop") {
            invokeVirtual {
                owner(VIDEO_ENGINE)
                name("setLooping")
            }
        }
