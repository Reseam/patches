// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.screenshot

import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.redditSettings

private const val BOOLEAN = "java.lang.Boolean"

val disableScreenshotBanner = patch("Disable screenshot banner") {
    description("No share prompt after taking a screenshot.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Sharing", RedditSettings.disableScreenshotBanner))

    execute {
        for (effect in bannerEffects) {
            effect.method("invokeSuspend")
                .point { invokeInterface { name("setValue") } }
                .captureArgumentAs("visible", 1)
                .before(RedditSettings.disableScreenshotBanner) { capture("visible").assign(staticField(booleanFalse)) }
        }
    }
}

private val bannerEffects = listOf(
    "com.reddit.sharing.screenshot.RedditScreenshotTriggerSharingListener\$ScreenshotBanner\$1\$1",
    "com.reddit.sharing.screenshot.composables.ScreenshotTakenBannerKt\$ScreenshotTakenBanner\$1\$1",
).map(::klass)

private val booleanFalse = field(BOOLEAN, "FALSE", BOOLEAN)
