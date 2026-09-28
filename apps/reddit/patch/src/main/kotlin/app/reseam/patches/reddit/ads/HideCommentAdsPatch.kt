// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.ads

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.redditSettings

private const val ADS_LOAD_COMPLETED = ", adsLoadCompleted="

val hideCommentAds = patch("Hide comment ads") {
    description("Stops loading ads between comments.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Ads", RedditSettings.hideCommentAds))

    execute {
        commentsAdState.method("<init>").after(RedditSettings.hideCommentAds) {
            thisObject.set(adsLoadCompleted, bool(true))
        }
    }
}

private val commentsAdState = klass("commentsAdState") { strings("CommentsAdState(conversationAdViewState=") }

private val adsLoadCompleted = commentsAdState.method("toString")
    .point { string(ADS_LOAD_COMPLETED); then(within = 6) { invokeVirtual { name("append"); params(Type.Boolean) } } }
    .writer(1)
    .field("adsLoadCompleted")
