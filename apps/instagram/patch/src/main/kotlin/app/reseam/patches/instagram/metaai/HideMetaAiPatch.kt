// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.metaai

import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.literal
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.MetaAiSettings
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val hideMetaAi = patch("Hide Meta AI") {
    description("Removes Meta AI from Explore search, DMs and the post menu.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(
        instagramSettings,
        section("Meta AI", MetaAiSettings.hideInExploreSearch, MetaAiSettings.hideInDirect, MetaAiSettings.hideInPosts),
    )

    execute {
        gate(MetaAiSettings.hideInExploreSearch) {
            klass(exploreMetaAiEnabled.owner).method("<init>") { hasParam(Type.List) }.after {
                thisObject.set(exploreMetaAiEnabled, bool(false))
            }
            exploreMetaAiSearch.alwaysReturn(false)
        }
        gate(MetaAiSettings.hideInDirect) {
            directMetaAiGate.alwaysReturn(false)
        }
        // Both DM search hints ("Search or ask Meta AI" and its reverse) follow the hint flag read; the
        // search screen and the inbox bar load them, so each load becomes the platform's "Search".
        val hintChoice = directSearchView.point { literal(DM_SEARCH_META_AI_HINT); then(within = 6) { resultOf(Type.Boolean) } }
        val firstHint = hintChoice.next { opcode(Opcode.CONST) }
        val metaAiHints = listOf(firstHint, firstHint.next { opcode(Opcode.CONST) }).map { it.instruction.literal!! }
        for (hint in metaAiHints) {
            methods("metaAiSearchHint$hint") { literals(*metaAiHints.toLongArray()) }
                .points { literal(hint) }
                .forEach {
                    gate(MetaAiSettings.hideInDirect) {
                        captureAs("metaAiHint", Type.Int)
                            .after { capture("metaAiHint").assign(int(PLATFORM_SEARCH_HINT)) }
                    }
                }
        }
        val shareButtonId = resources.id("id", "meta_ai_share_button")?.toLong() ?: error("id/meta_ai_share_button missing")
        // DM search always turns on the "send query to Meta AI thread" arrow this constructor sets up.
        val searchControllerSetup = method("searchControllerSetup") {
            inClass(searchController)
            literals(shareButtonId)
        }
        val shareButton = searchControllerSetup.point { literal(shareButtonId) }.next { opcode(Opcode.IPUT_OBJECT) }.field("metaAiShareButton")
        gate(MetaAiSettings.hideInDirect) {
            searchControllerSetup.after {
                thisObject.field(searchViewHolder).field(shareButton).callVirtual("android.view.View", "setVisibility", "(I)V", int(VIEW_GONE))
            }
        }
        for (flag in DM_SEARCH_META_AI_FLAGS) {
            methods("dmSearchFlag$flag") { literals(flag) }
                .points { literal(flag); then(within = 6) { resultOf(Type.Boolean) } }
                .forEach {
                    gate(MetaAiSettings.hideInDirect) {
                        captureAs("metaAiFlag", Type.Boolean)
                            .after { capture("metaAiFlag").assign(bool(false)) }
                    }
                }
        }
        gate(MetaAiSettings.hideInPosts) {
            postMenuMetaAi.alwaysReturnNull()
        }
    }
}

// MobileConfig parameter ids, stable across releases; each is read only where its surface is built.
// Explore search's Meta AI mode: the answer card in results, the sparkle field and the send-to-AI arrow.
private const val EXPLORE_META_AI_SEARCH = 0x0081069d00111fe8L
private const val DM_SEARCH_META_AI_HINT = 0x00810c29000a438cL
private const val PLATFORM_SEARCH_HINT = 0x0104000c // android.R.string.search_go
private const val VIEW_GONE = 8
private val DM_SEARCH_META_AI_FLAGS = listOf(
    0x00810420000b119dL, // send-to-AI arrow
    0x008104200004119aL, // "Ask Meta AI" suggestions while typing
    0x0081042000001199L, // Meta AI prompts on an empty query
)

private val exploreMetaAiSearch = method("exploreMetaAiSearch") { literals(EXPLORE_META_AI_SEARCH) }

private val directSearchView =
    klass("instagram.features.direct.inbox.fragment.DirectSearchInboxFragment").method("onCreateView")

private val searchController = klass("com.instagram.ui.widget.search.SearchController")
private val searchViewHolder = searchController.field("viewHolder")

// Builds the post menu's "About this post" summary and "Ask Meta AI" box; every caller skips a null block.
private val postMenuMetaAiStore = klass("postMenuMetaAiStore") { strings("IGMetaAIDiscoveryMenuQuery") }

private val postMenuMetaAiStoreOf = method("postMenuMetaAiStoreOf") {
    inClass(postMenuMetaAiStore)
    params(USER_SESSION)
}

private val postMenuMetaAi = method("postMenuMetaAi") {
    calls(postMenuMetaAiStoreOf)
    params(USER_SESSION, Type.String)
}

private val exploreMetaAiEnabled = method("exploreSearchBind") {
    inClass(klass("com.instagram.discovery.actionbar.ExploreActionBar"))
    strings("ExploreActionBar.maybeSetupMetaAIForSearch")
}.point { string("ExploreActionBar.maybeSetupMetaAIForSearch") }
    .previous { field { type(Type.Boolean) } }
    .field("exploreMetaAiEnabled")

private val metaAiGates = klass("metaAiGates") { strings("Ask Meta AI about this", "Summarize reel", "Find me similar reels") }

// The see-all DM search screen checks only the master gate; the other DM gates call it first.
private val directMetaAiGate = method("directMetaAiGate") {
    inClass(metaAiGates)
    params(USER_SESSION)
    returns(Type.Boolean)
    calledBy(klass("instagram.features.direct.inbox.fragment.DirectSearchInboxSeeAllFragment").method("onCreate"))
}
