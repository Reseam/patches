// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.ailabels

import app.reseam.patch.CodeScope
import app.reseam.patch.Type
import app.reseam.reddit.ailabels.bindings.RedditPosts

private const val LINK = "Lcom/reddit/domain/model/Link;"
private const val COMMENT = "Lcom/reddit/domain/model/Comment;"
private const val GALLERY = "Lcom/reddit/domain/model/PostGallery;"
private const val GALLERY_ITEM = "Lcom/reddit/domain/model/PostGalleryItem;"

// Reddit's Link and Comment keep their names and getters.
internal fun implementPostReaders() {
    RedditPosts.linkId.implement { returnValue(link("getKindWithId", Type.String)) }
    RedditPosts.linkTitle.implement { returnValue(link("getTitle", Type.String)) }
    RedditPosts.linkBody.implement { returnValue(link("getSelftext", Type.String)) }
    RedditPosts.linkPermalink.implement { returnValue(link("getPermalink", Type.String)) }
    RedditPosts.linkAuthorId.implement { returnValue(link("getAuthorId", Type.String)) }
    RedditPosts.linkAuthorName.implement { returnValue(link("getAuthor", Type.String)) }
    RedditPosts.linkCreatedSeconds.implement { returnValue(link("getCreatedUtc", Type.Long)) }
    RedditPosts.linkUrl.implement { returnValue(link("getUrl", Type.String)) }
    RedditPosts.promoted.implement { returnValue(link("getPromoted", Type.Boolean)) }
    RedditPosts.galleryItems.implement {
        val gallery = link("getGallery", GALLERY)
        whenNotNull(gallery) { returnValue(gallery.callVirtual(GALLERY, "getItems", "()${Type.List}")) }
        returnNull()
    }
    RedditPosts.galleryItemUrl.implement {
        returnValue(param(0).cast(GALLERY_ITEM).callVirtual(GALLERY_ITEM, "getUrl", "()${Type.String}"))
    }

    RedditPosts.isComment.implement {
        whenInstanceOf(param(0), COMMENT) { returnTrue() }
        returnFalse()
    }
    RedditPosts.commentId.implement { returnValue(comment("getKindWithId", Type.String)) }
    RedditPosts.commentPostId.implement { returnValue(comment("getLinkKindWithId", Type.String)) }
    RedditPosts.commentBody.implement { returnValue(comment("getBody", Type.String)) }
    RedditPosts.commentAuthorId.implement { returnValue(comment("getAuthorKindWithId", Type.String)) }
    RedditPosts.commentAuthorName.implement { returnValue(comment("getAuthor", Type.String)) }
    RedditPosts.commentCreatedSeconds.implement { returnValue(comment("getCreatedUtc", Type.Long)) }
}

private fun CodeScope.link(getter: String, type: String) = param(0).cast(LINK).callVirtual(LINK, getter, "()$type")

private fun CodeScope.comment(getter: String, type: String) =
    param(0).cast(COMMENT).callVirtual(COMMENT, getter, "()$type")
