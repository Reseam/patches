// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.nsfw

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.redditSettings

private const val FUNCTION0 = "kotlin.jvm.functions.Function0"

val skipNsfwWarning = patch("Skip NSFW community warning") {
    description("Opens mature communities without the confirmation dialog.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Communities", RedditSettings.skipNsfwWarning))

    execute {
        gate(RedditSettings.skipNsfwWarning) {
            showNsfwWarning.before {
                param(0).callInterface(FUNCTION0, "invoke", "()Ljava/lang/Object;")
                returnVoid()
            }
        }
    }
}

private val showNsfwWarning = method("showNsfwWarning") {
    strings("NSFW_POSITIVE_BUTTON_TEXT_ARG")
    params(FUNCTION0)
    returns(Type.Void)
}
