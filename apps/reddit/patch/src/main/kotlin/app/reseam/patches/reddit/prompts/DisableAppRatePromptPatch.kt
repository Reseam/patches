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
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.patches.reddit.core.unitInstance

val disableAppRatePrompt = patch("Disable rate-the-app prompt") {
    description("Stops Reddit from asking for a Play Store rating.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Prompts", RedditSettings.disableAppRatePrompt))

    execute {
        showAppRatePrompt.before(RedditSettings.disableAppRatePrompt) { returnValue(staticField(unitInstance)) }
    }
}

// The suspend function behind RedditAppRatePromptUseCase.showAppRatePromptWhenNeeded.
private val showAppRatePrompt = method("showAppRatePrompt") {
    calls(klass("com.reddit.apprate.usecase.RedditAppRatePromptUseCase\$showAppRatePromptWhenNeeded\$2").method("<init>"))
    returns(Type.Object)
}
