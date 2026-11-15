// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.ailabels

import app.reseam.patch.CodeScope
import app.reseam.patch.MethodLocal
import app.reseam.patch.MethodTarget
import app.reseam.patch.PointMatch
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.ValueRef
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patch.skipWhen
import app.reseam.patches.ailabels.AiLabelsPatchStrings
import app.reseam.patches.ailabels.aiLabelSettings
import app.reseam.patches.ailabels.aiLabelsSupport
import app.reseam.patches.pangram.pangramSupport
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.androidView
import app.reseam.patches.reddit.core.composerAtEntry
import app.reseam.patches.reddit.core.composerImpl
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.patches.reddit.core.unitInstance
import app.reseam.reddit.ailabels.bindings.RedditAiLabels
import app.reseam.reddit.labelfunctions.bindings.ImageLabelFactory
import app.reseam.reddit.labelfunctions.bindings.ImageLabelUpdate
import app.reseam.reddit.labelfunctions.bindings.TextLabelFactory
import app.reseam.reddit.labelfunctions.bindings.TextLabelUpdate
import app.reseam.reddit.settings.RedditStrings

val showAiLabels = patch(AiLabelsPatchStrings.showAiLabels) {
    description(AiLabelsPatchStrings.showAiLabelsDescription)
    compatibleWith(REDDIT)
    settings(redditSettings, section(RedditStrings.sectionAiLabels, *aiLabelSettings.toTypedArray()))

    execute {
        aiLabelsSupport()
        pangramSupport()
        implementPostReaders()
        implementLabelFunctions()

        readPosts()
        readComments()
        labelFeedHeaders()
        labelPostPageHeaders()
        labelCommentFooters()
        labelPhotos()
        labelFeedGalleryPages()
        labelPostPageGalleryItems()
    }
}

// Feeds show a short preview of a post's body, so labels use the whole posts Reddit loads: a feed page it keeps for
// opening posts, or the post it opens. The feed and the post page then check the same text.
private fun readPosts() {
    linkBatch.after { RedditAiLabels.onLinks(result) }

    // An experiment flag stops subreddit feeds from keeping their whole posts; its two reads answer false there.
    keptPostsFilter.points { invokeVirtual { owner(BOOLEAN); name("booleanValue") } }.all.forEach { flag ->
        flag.next { resultOf(Type.Boolean) }.captureAs("skipKeepingPosts", Type.Boolean)
            .after { capture("skipKeepingPosts").assign(bool(false)) }
    }
}

private fun readComments() {
    commentBatches.forEach { after { RedditAiLabels.onComments(result) } }
}

// Header rows draw their actions from models that do not name the post, so the header around them names it while
// it composes. Each text label sits just before the row's own buttons, so it lays out like them.
private fun labelFeedHeaders() {
    feedHeader.before {
        val element = thisObject.cast(feedHeaderSection.descriptor).field(feedHeaderSectionElement)
        RedditAiLabels.enterPost(element.callVirtual(feedHeaderElement.descriptor, "getLinkId", "()${Type.String}"))
    }
    feedHeader.after { RedditAiLabels.exitPost() }
    val optionsButton = feedHeaderActions.point { string("post_overflow") }
        .next { invokeStatic { returns(Type.Void); hasParam(FUNCTION0) } }
    textLabel(feedHeaderActions, listOf(optionsButton)) { call(RedditAiLabels.currentPost) }
}

private fun labelPostPageHeaders() {
    postPageHeader.before {
        val viewModel = thisObject.field(postPageViewModel)
        whenInstanceOf(viewModel, POST_DETAIL_VIEW_MODEL) {
            val arguments = viewModel.cast(POST_DETAIL_VIEW_MODEL).field(postPageArguments)
            RedditAiLabels.enterPost(arguments.field(argumentsPostId))
        }
    }
    postPageHeader.after { RedditAiLabels.exitPost() }
    // Join and the status badges.
    val trailingItems = postPageHeaderRow.point { invokeStatic { owner(postPageHeaderRow.owner); paramCount(6) } }
    textLabel(postPageHeaderRow, listOf(trailingItems)) { call(RedditAiLabels.currentPost) }
}

// The footer row orders its buttons by layout; the label goes before the options button in every order.
private fun labelCommentFooters() {
    textLabel(commentFooter, commentFooter.points { calls(commentOptionsButton) }.all) {
        paramOfType(commentFooterState.descriptor).field(commentFooterId)
    }
}

// Photos sit in top start boxes; each image label is the box's last child, so it draws on top, in the photo's top
// start corner.
private fun labelPhotos() {
    val composer = imageBox.composerAtEntry()
    val image = imageBox.points { invokeStatic { hasParam(imageProps.descriptor); paramCount(6) } }.all.last()
    // The image call's group ends, then the box.
    val boxEnd = image.next { composerEnd() }.next { composerEnd() }
    imageLabel(composer, boxEnd) { paramOfType(imageProps.descriptor).field(imagePropsUrl) }
}

private fun labelFeedGalleryPages() {
    val composer = feedGalleryPage.composerAtEntry(index = 1)
    val boxEnd = feedGalleryPage.point { string("post_image_gallery_item") }.next { outlinedEnds() }
    imageLabel(composer, boxEnd) { thisObject.field(feedGalleryPageElement).field(galleryPageMedia).field(mediaPath) }
}

// A post page gallery item's box holds a column with the photo, and one outlined call ends both. The call is
// replaced by its three ends, with the label after the column's and before the box's.
private fun labelPostPageGalleryItems() {
    val composer = postPageGalleryItem.composerAtEntry()
    val ends = postPageGalleryItem.points { outlinedEnds() }.all.last()
    val end = ends.callee("outlinedEnds").point { composerEnd() }.callee("composerEnd")
    ends.captureArgumentAs("endGroup", 1, Type.Boolean)
        .captureArgumentAs("endColumn", 2, Type.Boolean)
        .captureArgumentAs("endBox", 3, Type.Boolean)
        .before {
            val impl = local(composer).cast(composerImpl)
            fun end(flag: String) = impl.callVirtual(end.owner, end.name, end.proto, capture(flag))
            end("endGroup")
            end("endColumn")
            val update = newInstance(ImageLabelUpdate.descriptor, "(${Type.String})V", param(postPageGalleryItemUrl))
            androidView(newInstance(ImageLabelFactory.descriptor), update, local(composer))
            end("endBox")
        }
    ends.skipWhen { bool(true) }
}

private fun textLabel(composable: MethodTarget, beforeButtons: List<PointTarget>, id: CodeScope.() -> ValueRef) {
    val composer = composable.composerAtEntry()
    beforeButtons.forEach { point ->
        point.before {
            val update = newInstance(TextLabelUpdate.descriptor, "(${Type.String})V", id())
            androidView(newInstance(TextLabelFactory.descriptor), update, local(composer))
        }
    }
}

private fun imageLabel(composer: MethodLocal, boxEnd: PointTarget, url: CodeScope.() -> ValueRef) {
    boxEnd.before {
        val update = newInstance(ImageLabelUpdate.descriptor, "(${Type.String})V", url())
        androidView(newInstance(ImageLabelFactory.descriptor), update, local(composer))
    }
}

// Ends the current group or node: `end(isNode)` on the composer.
private fun PointMatch.composerEnd() = invokeVirtual { owner(composerImpl); params(Type.Boolean); returns(Type.Void) }

// R8 outlines a layout's closing into one static call making three `end(isNode)` calls.
private fun PointMatch.outlinedEnds() = invokeStatic { params(composerImpl, Type.Boolean, Type.Boolean, Type.Boolean) }

private fun implementLabelFunctions() {
    TextLabelFactory.invoke.implement { returnValue(RedditAiLabels.textLabel(param(0).cast(Type.Context))) }
    TextLabelUpdate.invoke.implement {
        RedditAiLabels.bindText(param(0).cast(Type.View), thisObject.fieldOfType(Type.String))
        returnValue(staticField(unitInstance))
    }
    ImageLabelFactory.invoke.implement { returnValue(RedditAiLabels.imageLabel(param(0).cast(Type.Context))) }
    ImageLabelUpdate.invoke.implement {
        RedditAiLabels.bindImage(param(0).cast(Type.View), thisObject.fieldOfType(Type.String))
        returnValue(staticField(unitInstance))
    }
}
