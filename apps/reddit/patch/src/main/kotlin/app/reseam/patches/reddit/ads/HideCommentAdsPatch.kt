// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.ads

import app.reseam.patch.ClassTarget
import app.reseam.patch.Type
import app.reseam.patch.descriptor
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.RedditPatchStrings
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.reddit.settings.RedditSettings
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.reddit.settings.RedditStrings

val hideCommentAds = patch(RedditPatchStrings.hideCommentAds) {
    description(RedditPatchStrings.hideCommentAdsDescription)
    compatibleWith(REDDIT)
    settings(redditSettings, section(RedditStrings.sectionAds, RedditSettings.hideCommentAds))

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
            // The comment store places comment ads from the loaded result, not from CommentsAdState.
            loadedAds.method("<init>").after {
                listOf(loadedCommentTreeAds, loadedCommentTreeAdModels)
                    .forEach { thisObject.set(it, newInstance(descriptor("java.util.LinkedHashMap"))) }
            }
        }
    }
}

private val commentsAdState = klass("commentsAdState") { strings("CommentsAdState(conversationAdViewState=") }

private fun stateField(label: String, appendType: String) = toStringField(commentsAdState, label, appendType)

private fun toStringField(owner: ClassTarget, label: String, appendType: String) = owner.method("toString")
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

private val loadedAds = klass("loadedAds") { strings("PostDetailAdLoaderResult(convoPlacementAd=") }

private val loadedCommentTreeAds = toStringField(loadedAds, ", commentTreeAds=", Type.Object)
private val loadedCommentTreeAdModels = toStringField(loadedAds, ", commentTeeAdPresentationModels=", Type.Object)

// The comments screen's initial state, the only constructor that builds a CommentsAdState.
private val commentsInitialState = method("commentsInitialState") {
    name("<init>")
    calls(commentsAdState.method("<init>"))
}

private val noCommentTreeAds = commentsInitialState.point { calls(commentsAdState.method("<init>")) }
    .writer(6)
    .field("noCommentTreeAds")
