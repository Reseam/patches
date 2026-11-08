// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.media

import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val showSensitiveMedia = patch("Show sensitive media") {
    description("Shows sensitive media in timelines, quotes, notifications and the media viewer without the blur or confirmation prompt.")
    compatibleWith(X)
    settings(xSettings, section(XSettingsPages.Media, XStrings.sectionMedia, XSettings.showSensitiveMedia))

    execute {
        gate(XSettings.showSensitiveMedia) {
            val writes = mediaVisibilityWrites.all
            require(writes.isNotEmpty()) { "no ContextualPost constructor stores its media visibility" }
            // A post without media visibility results shows its media as is.
            writes.forEach { write ->
                write.captureAs("visibility", mediaVisibilityResults.descriptor).before { capture("visibility").assign(nullObject) }
            }
        }
    }
}

private val contextualPost = klass("contextualPost") { strings("ContextualPost(canonicalPost=", ", quotedPost=") }

private val mediaVisibilityResults = klass("mediaVisibilityResults") { strings("MediaVisibilityResults(blurImageInterstitial=") }

private val mediaVisibilityWrites = contextualPost
    .methods("contextualPostConstructors") {
        name("<init>")
        hasParam(mediaVisibilityResults.descriptor)
    }
    .points("mediaVisibilityWrites") {
        opcode(Opcode.IPUT_OBJECT)
        field {
            owner(contextualPost.descriptor)
            type(mediaVisibilityResults.descriptor)
        }
    }
