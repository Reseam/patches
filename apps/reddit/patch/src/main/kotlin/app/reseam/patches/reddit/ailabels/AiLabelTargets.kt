// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.ailabels

import app.reseam.patch.ClassTarget
import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.dex.descriptor
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.methodRef
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point

internal const val BOOLEAN = "Ljava/lang/Boolean;"
internal const val FUNCTION0 = "Lkotlin/jvm/functions/Function0;"
internal const val POST_DETAIL_VIEW_MODEL = "Lcom/reddit/postdetail/refactor/RedditPostDetailViewModel;"

// Where posts and comments come from.

// The link data source finishes each batch of posts it maps from GraphQL with a static helper. The data source is
// renamed; its coroutine classes keep their names and point at it.
private val linkDataSource = fieldTarget("linkDataSource") {
    klass("Lcom/reddit/data/remote/RemoteGqlLinkDataSource\$getPostsByIds\$1;").classDef.instanceFields
        .single { it.name == "this\$0" }.ref
}
internal val linkBatch = method("linkBatch") {
    inClass(klass(linkDataSource.type))
    returns(Type.List)
    params(Type.List, Type.Boolean)
}

// Picks the posts of a loaded feed page whose whole post Reddit fetches and keeps.
internal val keptPostsFilter = method("keptPostsFilter") {
    params(Type.List, "Lcom/reddit/feeds/data/FeedType;")
    returns(Type.ArrayList)
    calls { owner(BOOLEAN); name("booleanValue") }
}

internal val commentBatches = methods("commentBatches") {
    inClass(klass("Lcom/reddit/data/model/graphql/GqlCommentToCommentDomainModelMapper;"))
    returns(Type.List)
    custom { name == "mapToDomainModels" || name == "mapToDomainModelsWithPostInfo" }
}

// Feed header: the section holding the post's element, and the row of actions it draws.

internal val feedHeaderSection = klass("metadataHeaderSection") { strings("MetadataHeaderSection(data=") }
internal val feedHeaderElement = klass("metadataHeaderElement") { strings("MetadataHeaderElement(linkId=") }
internal val feedHeaderSectionElement = fieldTarget("metadataHeaderSectionElement") {
    feedHeaderSection.classDef.instanceFields.single { it.fieldType == feedHeaderElement.descriptor }.ref
}

// A feed section draws itself in its one implementation of the feed section interface's composable.
private val feedSectionContent get() =
    klass(feedHeaderSection.classDef.interfaces.single()).classDef.virtualMethods.single { it.returnType == Type.Void }
internal val feedHeader = method("metadataHeaderContent") {
    inClass(feedHeaderSection)
    custom { name == feedSectionContent.name && parameterTypes == feedSectionContent.parameterTypes }
}
internal val feedHeaderActions = method("feedHeaderActions") {
    strings("post_overflow")
    hasParam("Lcom/reddit/feeds/ui/composables/HeaderStyle;")
}

// Post page header: the row, the element composable that tags it and calls it, and the lambda handing that
// element the page's view model.

private val postPageHeaderTagged = methods("postPageHeaderTagged") { strings("post_header_info") }
internal val postPageHeaderRow = postPageHeaderTagged.single { postPageHeaderTagged.all.any { it.callsMethod(this) } }
private val postPageHeaderElement = postPageHeaderTagged.single { callsMethod(postPageHeaderRow) }
private val postPageViewModelTypes get() = klass(POST_DETAIL_VIEW_MODEL).classDef.interfaces

// The element's own recompose lambda calls it too, without the view model.
internal val postPageHeader = method("postPageHeaderContent") {
    name("invoke")
    calls(postPageHeaderElement)
    custom { classDef.instanceFields.any { it.fieldType in postPageViewModelTypes } }
}
internal val postPageViewModel = fieldTarget("postPageViewModel") {
    klass(postPageHeader.owner).classDef.instanceFields.single { it.fieldType in postPageViewModelTypes }.ref
}
private val postPageArgumentsClass = klass("postDetailScreenArguments") {
    strings("PostDetailScreenArguments(analyticsScreenData=")
}
internal val postPageArguments = fieldTarget("postDetailScreenArguments") {
    klass(POST_DETAIL_VIEW_MODEL).classDef.instanceFields
        .single { it.fieldType == postPageArgumentsClass.descriptor }.ref
}
internal val argumentsPostId = fieldAfter(postPageArgumentsClass, ", linkKindWithId=")

// Comment footer: its state names the comment, and its row holds the options button.

internal val commentFooterState = klass("commentFooterViewState") { strings("CommentFooterViewState(score=") }
internal val commentFooterId = fieldAfter(commentFooterState, ", commentIdWithKind=")
internal val commentOptionsButton = method("commentOptionsButton") { strings("comment_overflow_button") }
internal val commentFooter = method("commentFooter") {
    hasParam(commentFooterState.descriptor)
    calls(commentOptionsButton)
}

// Photos: the image component's top start box around its loading, image and error states.

internal val imageProps = klass("imageComponentProps") { strings("ImageComponentProps(url=") }
internal val imagePropsUrl = fieldAfter(imageProps, "ImageComponentProps(url=")
internal val imageBox = method("imageComponentBox") {
    hasParam(imageProps.descriptor)
    calls { hasParam(imageProps.descriptor); paramCount(6); returns(Type.Void) }
}

// Feed gallery: one page is a lambda holding the page's element, whose media source has the link.

internal val feedGalleryPage = method("feedGalleryPage") { strings("post_image_gallery_item") }
private val galleryPage = klass("galleryPageElement") { strings("GalleryPageElement(mediaSource=") }
internal val feedGalleryPageElement = fieldTarget("feedGalleryPageElement") {
    klass(feedGalleryPage.owner).classDef.instanceFields.single { it.fieldType == galleryPage.descriptor }.ref
}
internal val galleryPageMedia = fieldAfter(galleryPage, "GalleryPageElement(mediaSource=")
private val cellMediaSource = klass("cellMediaSource") { strings("CellMediaSourceElement(path=") }
internal val mediaPath = fieldAfter(cellMediaSource, "CellMediaSourceElement(path=")

// Post page gallery: one item's parameters are its media id, then its image's link.

internal val postPageGalleryItem = method("postPageGalleryItem") { strings("post_media_gallery_item") }
internal val postPageGalleryItemUrl: Int
    get() = postPageGalleryItem.parameterTypes.withIndex().filter { it.value == Type.String }[1].index

private fun MethodTarget.callsMethod(target: MethodTarget) =
    method.instructions.any { it.methodRef?.descriptor == target.descriptor }

/** The field [owner]'s toString prints after [label]; R8 can fold a constant field into the label before it. */
private fun fieldAfter(owner: ClassTarget, label: String) =
    method("toString after $label") {
        inClass(owner)
        name("toString")
    }.point { stringContains(label) }.next { where { fieldRef != null } }.field()
