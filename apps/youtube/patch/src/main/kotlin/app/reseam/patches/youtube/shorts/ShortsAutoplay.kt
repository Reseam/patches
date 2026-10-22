// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.shorts

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.enumConstant
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.ShortsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.shorts.bindings.ShortsAutoplay

private val repeatBehavior = klass("Shorts repeat behavior") {
    enumConstants("REEL_LOOP_BEHAVIOR_UNKNOWN", "REEL_LOOP_BEHAVIOR_REPEAT")
}

// Older releases predate the auto-advance enum and need the next-Short action restored.
private val nativeAutoAdvance = repeatBehavior.method("<clinit>").points("native Shorts auto advance") {
    string("REEL_LOOP_BEHAVIOR_AUTO_ADVANCE")
}

private val decodeRepeatBehavior = method("decode Shorts repeat behavior") {
    inClass(repeatBehavior)
    params(Type.Int)
    returns(repeatBehavior.descriptor)
}

private val shortsRepeatPolicy = method("Shorts repeat policy") {
    calls(decodeRepeatBehavior)
    returns(repeatBehavior.descriptor)
}

private val playbackEndListener = klass("Shorts playback end listener") {
    strings("Reels[%s] Playback Time: %d ms")
}

// The end listener waits for the pending playback before applying the repeat behavior.
private val legacyPlaybackEnd = method("legacy Shorts playback end") {
    inClass(playbackEndListener)
    returns(Type.Void)
    paramCount(1)
    calls { owner("com.google.common.util.concurrent.ListenableFuture"); name("isDone") }
    calls(decodeRepeatBehavior)
}

private val nextShortAction = legacyPlaybackEnd.point("Shorts user action constructor") {
    invokeDirect { name("<init>"); paramCount(3); hasParam(Type.Int); hasParam(Type.Object) }
}.callee("Shorts user action constructor")

private val nextShort = legacyPlaybackEnd.point("Shorts sequence action") {
    invokeVirtual { params(nextShortAction.owner); returns(Type.Int) }
}.callee("Shorts sequence action")

private val legacyController = legacyPlaybackEnd.point("Shorts sequence controller") {
    opcode(Opcode.IGET_OBJECT)
    field { type(nextShort.owner) }
}.field()

val shortsAutoplay = patch("Shorts autoplay") {
    description("Automatically plays the next Short when Shorts autoplay is enabled.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Shorts, YouTubeStrings.sectionShorts, ShortsSettings.shortsAutoplay, ShortsSettings.shortsAutoplayBackground))

    execute {
        mainActivityOnCreate.before { ShortsAutoplay.setMainActivity(thisObject) }
        if (nativeAutoAdvance.all.isNotEmpty()) {
            shortsRepeatPolicy.after {
                result.assign(
                    ShortsAutoplay.changeRepeatBehavior(result).cast(repeatBehavior.descriptor),
                )
            }
        } else {
            legacyPlaybackEnd.points("legacy Shorts repeat behavior") {
                invokeStatic { owner(repeatBehavior.descriptor); params(Type.Int); returns(repeatBehavior.descriptor) }
            }.forEach {
                next { resultOf(repeatBehavior.descriptor) }.captureAs("behavior", repeatBehavior.descriptor).after {
                    whenTrue(ShortsAutoplay.shouldAutoplay()) {
                        thisObject.field(legacyController).call(
                            nextShort,
                            newInstance(nextShortAction.owner, nextShortAction.proto, int(3), nullObject, nullObject),
                        )
                        returnVoid()
                    }
                    capture("behavior").assign(staticField(repeatBehavior.enumConstant("REEL_LOOP_BEHAVIOR_REPEAT")))
                }
            }
        }
    }
}
