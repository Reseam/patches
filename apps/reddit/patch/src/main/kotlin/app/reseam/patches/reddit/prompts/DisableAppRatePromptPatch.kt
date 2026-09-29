// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.prompts

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.overrideIntConfigs
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.patches.reddit.core.unitInstance

// A hundred years; Int.MAX_VALUE days overflows Instant.plus.
private const val COOLDOWN_DAYS = 36_500

val disableAppRatePrompt = patch("Disable rate-the-app prompt") {
    description("Stops Reddit from asking for a Play Store rating.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Prompts", RedditSettings.disableAppRatePrompt))

    execute {
        showAppRatePrompt.before(RedditSettings.disableAppRatePrompt) { returnValue(staticField(unitInstance)) }
        overrideIntConfigs(RedditSettings.disableAppRatePrompt, mapOf(
            "android_app_rater_open_thresh" to Int.MAX_VALUE,
            "android_app_rater_action_thresh" to Int.MAX_VALUE,
            "android_app_rater_v2_action_thresh" to Int.MAX_VALUE,
            "android_app_rater_cooldown_days" to COOLDOWN_DAYS,
        ))
    }
}

// The suspend function behind RedditAppRatePromptUseCase.showAppRatePromptWhenNeeded.
private val showAppRatePrompt = method("showAppRatePrompt") {
    calls(klass("com.reddit.apprate.usecase.RedditAppRatePromptUseCase\$showAppRatePromptWhenNeeded\$2").method("<init>"))
    returns(Type.Object)
}
