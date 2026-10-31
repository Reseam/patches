// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.comments

import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.reddit.settings.RedditSettings
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.reddit.settings.RedditStrings

val ignoreSuggestedSort = patch("Always use my default comment sort") {
    description("Ignores the comment sort a post or community suggests and uses your default sort.")
    compatibleWith(REDDIT)
    settings(redditSettings, section(RedditStrings.sectionComments, RedditSettings.ignoreSuggestedSort))

    execute {
        gate(RedditSettings.ignoreSuggestedSort) {
            klass("com.reddit.domain.model.AccountPreferences").method("getIgnoreSuggestedSort").alwaysReturn(true)
        }
    }
}
