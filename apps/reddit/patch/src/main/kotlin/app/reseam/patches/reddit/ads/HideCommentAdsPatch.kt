// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.ads

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.redditSettings

val hideCommentAds = patch("Hide comment ads") {
    description("Removes the ads under a post and between its comments.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Ads", RedditSettings.hideCommentAds))

    execute {
        // Every state the comments screen holds is built here, copies included, so each one
        // leaves with the ad slots of the empty initial state.
        gate(RedditSettings.hideCommentAds) {
            commentsAdState.method("<init>").after {
                listOf(conversationAd, conversationAdLink, afterCommentsAd, afterCommentsAdLink)
                    .forEach { thisObject.set(it, nullObject) }
                thisObject.set(commentTreeAds, staticField(noCommentTreeAds))
                thisObject.set(adPlaceholderEligible, bool(false))
                thisObject.set(adsLoadCompleted, bool(true))
            }
        }
    }
}

private val commentsAdState = klass("commentsAdState") { strings("CommentsAdState(conversationAdViewState=") }

private fun stateField(label: String, appendType: String) = commentsAdState.method("toString")
    .point { string(label); then(within = 6) { invokeVirtual { name("append"); params(appendType) } } }
    .writer(1)
    .field(label.substringAfter('(').trim(',', ' ', '='))

private val conversationAd = stateField("CommentsAdState(conversationAdViewState=", Type.Object)
private val adPlaceholderEligible = stateField(", wasAdPlaceholderEligible=", Type.Boolean)
private val conversationAdLink = stateField(", conversationAdLink=", Type.Object)
private val afterCommentsAd = stateField(", afterCommentsAdViewState=", Type.Object)
private val afterCommentsAdLink = stateField(", afterCommentsAdLink=", Type.Object)
private val commentTreeAds = stateField(", commentTreeAds=", Type.Object)
private val adsLoadCompleted = stateField(", adsLoadCompleted=", Type.Boolean)

// The comments screen's initial state, the only constructor that builds a CommentsAdState.
private val commentsInitialState = method("commentsInitialState") {
    name("<init>")
    calls(commentsAdState.method("<init>"))
}

private val noCommentTreeAds = commentsInitialState.point { calls(commentsAdState.method("<init>")) }
    .writer(6)
    .field("noCommentTreeAds")
