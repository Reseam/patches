// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.comments

import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.returnTrueWhen
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.redditSettings

val ignoreSuggestedSort = patch("Always use my default comment sort") {
    description("Ignores the comment sort a post or community suggests and uses your default sort.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Comments", RedditSettings.ignoreSuggestedSort))

    execute {
        klass("com.reddit.domain.model.AccountPreferences").method("getIgnoreSuggestedSort").returnTrueWhen(RedditSettings.ignoreSuggestedSort)
    }
}
