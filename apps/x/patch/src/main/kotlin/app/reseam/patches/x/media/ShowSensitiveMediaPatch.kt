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
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

private val contextualPost = klass("contextualPost") {
    strings("ContextualPost(canonicalPost=", ", quotedPost=")
}

private val mediaVisibilityResults = klass("mediaVisibilityResults") {
    strings("MediaVisibilityResults(blurImageInterstitial=")
}

// The two constructors that take the visibility wrapper: the wire deserializer's default-args
// constructor and the one the database, detail and copy paths call.
private val mediaVisibilityWrites = contextualPost
    .methods("mediaVisibilityWriters") {
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

val showSensitiveMedia = patch("Show sensitive media") {
    description("Shows sensitive media in timelines, quotes, notifications and the media viewer without the blur or confirmation prompt.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionMedia, XSettings.showSensitiveMedia))

    execute {
        gate(XSettings.showSensitiveMedia) {
            mediaVisibilityWrites.forEach {
                captureAs("mediaVisibilityResults", mediaVisibilityResults.descriptor).before {
                    capture("mediaVisibilityResults").assign(nullObject)
                }
            }
        }
    }
}
