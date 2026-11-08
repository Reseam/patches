// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.postdetail.bindings.ReplySort
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val defaultReplySort = patch("Default reply sort") {
    description("Sorts the replies of a post you open by your choice. Picking another sort on the post still works.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionTimeline, XSettings.defaultReplySort, XSettings.rememberReplySort))

    execute {
        // Post detail starts the conversation prefetch, the timeline repository and the header's
        // sort label each from their own Relevance read; a sort picked there updates the
        // repository and the label.
        for (start in listOf(conversationPrefetch, conversationTimeline, replySortLabel)) {
            start.point { field { owner(replySort.descriptor); name("Relevance") } }
                .captureAs("sort", replySort.descriptor)
                .after { capture("sort").assign(ReplySort.initial(capture("sort")).cast(replySort.descriptor)) }
        }

        gate(XSettings.rememberReplySort) {
            changeReplySorting.point { checkCast(replySort.descriptor) }
                .captureAs("sort", replySort.descriptor)
                .after { ReplySort.remember(capture("sort")) }
        }
    }
}

private val replySort = klass("replySort") { enumConstants("Relevance", "Recency", "Likes") }

private val conversationPrefetch = method("conversationPrefetch") {
    strings("PrefetchedConversationRequest", "Conversation request started for post ")
    accesses { owner(replySort.descriptor); name("Relevance") }
}

// Builds the timeline repository and binds the header's sort callback by Kotlin's kept reference name.
private val conversationTimeline = method("conversationTimeline") {
    strings("changeReplySorting(Lcom/x/models/TimelineRankingMode;)V")
}

// The header's `rememberSaveable { mutableStateOf(Relevance) }` initializer.
private val replySortLabel = method("replySortLabel") {
    name("invoke")
    paramCount(0)
    accesses { owner(replySort.descriptor); name("Relevance") }
}

// The changeReplySorting reference, inlined into a merged lambda: it casts the picked sort, then stores it in the
// repository. The other user of both lateinit names is the coroutine that builds them.
private val changeReplySorting = method("changeReplySorting") {
    name("invoke")
    strings("timelineRepository", "defaultUrtTimelineComponent")
}
