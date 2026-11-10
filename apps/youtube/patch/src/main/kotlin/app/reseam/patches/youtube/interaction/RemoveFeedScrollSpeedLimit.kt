// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.FeedSettings
import app.reseam.youtube.settings.YouTubeStrings

private val feedList = klass("Lcom/google/android/apps/youtube/app/common/rendering/SnappyRecyclerView;")

// Feeds clamp forward flings to a server-sent velocity, well under the platform maximum. 20.26 has no clamp.
private val flingVelocityClamp = methods("feed fling velocity clamp") {
    inClass(feedList)
    params(Type.Int)
    returns(Type.Int)
    calls { owner("Ljava/lang/Math;"); name("min") }
}

val removeFeedScrollSpeedLimit = patch(YouTubePatchStrings.removeFeedScrollSpeedLimit) {
    description(YouTubePatchStrings.removeFeedScrollSpeedLimitDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Feed, YouTubeStrings.sectionFeed, FeedSettings.removeFeedScrollSpeedLimit))

    execute {
        gate(FeedSettings.removeFeedScrollSpeedLimit) {
            flingVelocityClamp.all.forEach { clamp -> clamp.before { returnValue(param(0)) } }
        }
    }
}
