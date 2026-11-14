// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.MethodTarget
import app.reseam.patch.MethodsTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods

// The Compose model every post on every screen becomes before it is drawn.
val availablePost = klass("availablePost") { strings("AvailablePost(entryId=") }

val postOptionsState = klass("postOptionsState") { strings("PostOptionsState(showOptionsDialog=") }

val mediaPhoto = klass("mediaPhoto") { strings("MediaContentImage(mediaId=") }

/** The composables that draw a post's header, and the options button each one ends with. */
data class PostHeader(val optionsButton: MethodTarget, val headers: MethodsTarget)

fun PatchRuntime.postHeader(): PostHeader {
    val optionsLabel = resources.id("string", "button_action_options_tweet") ?: error("string/button_action_options_tweet missing")
    val optionsButton = method("postOptionsButton") {
        literals(optionsLabel.toLong())
        param(0, postOptionsState.descriptor)
    }
    val headers = methods("postHeaders") {
        param(0, availablePost.descriptor)
        calls(optionsButton)
    }
    return PostHeader(optionsButton, headers)
}
