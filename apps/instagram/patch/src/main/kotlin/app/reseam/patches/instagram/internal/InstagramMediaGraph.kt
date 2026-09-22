// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.internal

import app.reseam.patch.Type
import app.reseam.patch.bind
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.isSet
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patches.instagram.core.EXTENDED_IMAGE_URL
import app.reseam.patches.instagram.core.FRAGMENT_ACTIVITY
import app.reseam.patches.instagram.core.MEDIA_OPTION
import app.reseam.patches.instagram.core.REEL_ITEM
import app.reseam.patches.instagram.core.VIDEO_VERSION_INTF

/** How Instagram's media value class, menus, and story sheets are found, shared by the media patches. */
object InstagramMediaGraph {
    val feedMenuCreator = klass("feedMenuCreator") {
        strings("MediaOptionsOverflowMenuCreator")
    }

    val feedMenuRow = klass("feedMenuRow") {
        hasInstanceField(MEDIA_OPTION)
        hasInstanceField(Type.CharSequence)
        hasInstanceField(Type.Boolean)
        custom { instanceFields.size < 10 }
    }

    val feedMenuRowLabel = fieldTarget("feedMenuRowLabel") {
        feedMenuRow.classDef.instanceFields.single { it.fieldType == "Ljava/lang/CharSequence;" }.ref
    }

    val feedMenuAddItem = method("feedMenuAddItem") {
        inClass(feedMenuCreator)
        returns(Type.Void)
        params(MEDIA_OPTION, feedMenuCreator.descriptor, Type.ArrayList, Type.Int)
    }

    val feedMenuAppendRow = method("feedMenuAppendRow") {
        inClass(feedMenuCreator)
        returns(Type.Void)
        paramCount(6)
        hasParam(MEDIA_OPTION)
        hasParam(feedMenuCreator.descriptor)
        hasParam(Type.CharSequence)
        hasParam(Type.ArrayList)
        hasParam(Type.Boolean)
    }

    val feedClickHandler = method("feedClickHandler") {
        strings("MediaOptionsOverflowHelper")
        returns(Type.Void)
        params(MEDIA_OPTION)
    }

    val feedMediaGetter = method("feedMediaGetter") {
        inClass(klass(feedClickHandler.owner))
        calledBy(feedClickHandler)
        returns("com.instagram.feed.media.Media")
        params(feedClickHandler.owner)
        opcode(Opcode.IGET_OBJECT)
    }

    val feedCarouselIndex = fieldTarget("feedCarouselIndex") {
        feedClickHandler.method.instructions.firstNotNullOfOrNull { instruction ->
            instruction.fieldRef?.takeIf { instruction.opcode == Opcode.IGET && it.definingClass == feedClickHandler.owner && it.fieldType == Type.Int }
        } ?: error("Could not find the feed carousel index")
    }

    val feedSimplifiedMenu = method("feedSimplifiedMenu") {
        strings("SimplifiedMediaOverflowBottomSheet")
        returns(Type.Void)
        hasParam("com.instagram.feed.media.Media")
        hasParam(Type.View)
    }

    val feedSimplifiedFilter = method("feedSimplifiedFilter") {
        calledBy(feedSimplifiedMenu)
        returns(Type.List)
        params(Type.List, Type.Boolean)
    }

    val feedSimplifiedAllowedOptions = method("feedSimplifiedAllowedOptions") {
        inClass(klass(feedSimplifiedFilter.owner))
        calledBy(feedSimplifiedFilter)
        returns(Type.List)
        params(Type.Boolean)
    }

    val reelsHelper = klass("reelsHelper") {
        hasInstanceField("com.instagram.feed.media.Media")
        hasInstanceField(FRAGMENT_ACTIVITY)
        hasInstanceField("com.instagram.clips.intf.ClipsViewerConfig")
    }

    val reelsRowAdder = method("reelsRowAdder") {
        inClass(reelsHelper)
        returns(Type.Void)
        paramCount(4)
        param(0, Type.Context)
        param(1, MEDIA_OPTION)
    }

    val reelsClickHandler = method("reelsClickHandler") {
        inClass(reelsHelper)
        strings("android_purge_26_q3_ClipsOrganicMoreOptionsHelper_handleOptionSelected")
        returns(Type.Void)
        params(MEDIA_OPTION)
    }

    val storyActionSheet = klass("storyActionSheet") {
        strings("archive_highlight_option", "copy_link_url", "delete_photo_title")
    }

    val storyLabelArrays = methods("storyLabelArrays") {
        inClass(storyActionSheet)
        returns("java.lang.CharSequence[]")
        hasParam(storyActionSheet.descriptor)
    }

    val storyDispatchers = methods("storyDispatchers") {
        inClass(storyActionSheet)
        returns(Type.Void)
        hasParam(storyActionSheet.descriptor)
        hasParam(Type.CharSequence)
    }

    val media = bind("media") {
        fromClass(klass("com.instagram.feed.media.Media"))
        string("videoUrl") {
            listGetter("video_versions") {
                rankBy("callers followed by cast to VideoVersionIntf") { callSitesFollowedByCast(VIDEO_VERSION_INTF) }
            }
            first()
            cast(VIDEO_VERSION_INTF)
            callInterface(VIDEO_VERSION_INTF, "getUrl", "()Ljava/lang/String;")
        }
        objectValue("carouselChildren") {
            listGetter("carousel_media") {
                rankBy("callers followed by cast to media type") { callSitesFollowedByCast(sourceType) }
            }
        }
    }

    val imageInfoGetter = method("imageInfoGetter") {
        inClass(klass("com.instagram.feed.media.Media"))
        strings("image_versions2")
        returns("com.instagram.model.mediasize.ImageInfo")
        paramCount(0)
    }

    val imageInfo = bind("imageInfo") {
        fromClass(klass("com.instagram.model.mediasize.ImageInfo"))
        objectValue("candidates") {
            listGetter("image_versions2 candidates") {
                rankBy("extended image URLs") { callSitesFollowedByCast(EXTENDED_IMAGE_URL) }
            }
        }
    }

    val reelItemClass = klass(REEL_ITEM)

    val reelItemMediaField = fieldTarget("reelItemMediaField") {
        val mediaType = "Lcom/instagram/feed/media/Media;"
        val candidates = reelItemClass.classDef.instanceFields.filter { it.fieldType == mediaType && AccessFlags.FINAL.isSet(it.accessFlags) }
        val field = candidates.singleOrNull()
            ?: error("Expected exactly one final $mediaType field on $REEL_ITEM, found ${candidates.size} (${candidates.joinToString { it.name }})")
        field.ref
    }

}
