// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.feed

import app.reseam.patch.klass
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.FeedElements
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.cellGroupMapper
import app.reseam.patches.reddit.core.feedElementClassNames
import app.reseam.patches.reddit.core.redditSettings

val hideRecommendedPosts = patch("Hide recommended posts") {
    description("Removes suggested posts from communities you do not follow in Home.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Feed", RedditSettings.hideRecommendedPosts))

    execute {
        val postElement = feedElementClassNames(listOf("PostElement(linkId="))
        // A cell group carries a recommendation context only when Reddit picked it for you.
        gate(RedditSettings.hideRecommendedPosts) {
            cellGroupMapper.after {
                whenNotNull(paramOfType(recommendationContext.descriptor)) {
                    whenTrue(call(FeedElements.isAny, capture("result"), string(postElement))) { returnNull() }
                }
            }
        }
    }
}

private val recommendationContext = klass("recommendationContext") { strings("CellGroupRecommendationContext(name=") }
