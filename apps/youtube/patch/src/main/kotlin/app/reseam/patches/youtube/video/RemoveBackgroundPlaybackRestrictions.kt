// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.after
import app.reseam.patch.settings.returnTrueWhen
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.booleanFeatureReads

private val backgroundPlaybackManager = method("background playback policy") {
    literals(64657230L)
    returns(Type.Boolean)
    paramCount(1)
}

private val backgroundPlaybackManagerShorts = method("Shorts background playback policy") {
    literals(151635310L)
    returns(Type.Boolean)
    opcode(Opcode.IGET_BOOLEAN)
}

private val kidsPlaybackPolicy = method("kids background playback policy") {
    inClass(klass("kids playback policy controller") { strings("MPPC") })
    returns(Type.Void)
    paramCount(3)
    param(0, Type.Int)
}

val removeBackgroundPlaybackRestrictions = patch("Remove background playback restrictions") {
    description("Allows eligible videos and Shorts to continue playing in the background.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Video, "Background playback", YouTubeSettings.removeBackgroundPlaybackRestrictions, YouTubeSettings.allowShortsBackgroundPlayback),
    )

    execute {
        val categoryId = resources.id("string", "pref_background_and_offline_category")?.toLong()
            ?: error("string/pref_background_and_offline_category is missing")
        val settingsMethod = method("background playback settings category") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            returns(Type.String)
            params()
            literals(categoryId)
        }
        val settingsBoolean = settingsMethod.point("background setting boolean") {
            invokeVirtual { returns(Type.Boolean) }
        }.callee("background setting boolean method")

        backgroundPlaybackManager.after(YouTubeSettings.removeBackgroundPlaybackRestrictions) {
            capture("result").assign(bool(true))
        }
        backgroundPlaybackManagerShorts.after {
            whenEnabled(YouTubeSettings.removeBackgroundPlaybackRestrictions) { capture("result").assign(bool(true)) }
            whenEnabled(YouTubeSettings.allowShortsBackgroundPlayback) { capture("result").assign(bool(true)) }
        }
        settingsBoolean.returnTrueWhen(YouTubeSettings.removeBackgroundPlaybackRestrictions)
        booleanFeatureReads(45415425L).forEach { site ->
            site.next { resultOf(Type.Boolean) }.captureAs("enabled", Type.Boolean)
                .after(YouTubeSettings.removeBackgroundPlaybackRestrictions) { capture("enabled").assign(bool(true)) }
        }
        // Keep the background-compatible player implementation.
        booleanFeatureReads(45698813L).forEach { site ->
            site.next { resultOf(Type.Boolean) }.captureAs("enabled", Type.Boolean).after {
                capture("enabled").assign(bool(false))
            }
        }
        kidsPlaybackPolicy.skipWhen(YouTubeSettings.removeBackgroundPlaybackRestrictions)
    }
}
