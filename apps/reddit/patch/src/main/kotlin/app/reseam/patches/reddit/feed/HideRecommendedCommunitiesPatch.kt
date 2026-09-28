// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.feed

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.feedSectionContent
import app.reseam.patches.reddit.core.redditSettings

val hideRecommendedCommunities = patch("Hide recommended communities") {
    description("Removes the community suggestions shelf from feeds.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Feed", RedditSettings.hideRecommendedCommunities))

    execute {
        recommendationSections.forEach { it.skipWhen(RedditSettings.hideRecommendedCommunities) }
    }
}

// "recomendation" is Reddit's spelling.
private val recommendationSections = listOf("", "card_post_", "compact_post_", "list_style_")
    .map { layout -> feedSectionContent("${layout}community_recomendation_section_") }
