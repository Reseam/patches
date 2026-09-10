// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.internal

import app.reseam.patch.Type
import app.reseam.patch.bind
import app.reseam.patch.classTarget
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
import app.reseam.patch.point
import app.reseam.patches.instagram.core.EXTENDED_IMAGE_URL
import app.reseam.patches.instagram.core.FRAGMENT_ACTIVITY
import app.reseam.patches.instagram.core.MEDIA_OPTION
import app.reseam.patches.instagram.core.REEL_ITEM
import app.reseam.patches.instagram.core.VIDEO_VERSION_INTF

/** How Instagram's media value class, menus, and story sheets are found, shared by the media patches. */
object InstagramMediaGraph {
    val feedMenuBuilder = method("feedMenuBuilder") {
        strings("instagram_feed_self_view_overflow_menu_insights_option_impression")
        returns(Type.Object)
    }

    val feedMenuCreator = klass("feedMenuCreator") {
        strings("MediaOptionsOverflowMenuCreator")
    }

    val feedMenuAddItem = method("feedMenuAddItem") {
        inClass(feedMenuCreator)
        returns(Type.Void)
        params(MEDIA_OPTION, feedMenuCreator.descriptor, Type.ArrayList, Type.Int)
    }

    val feedClickHandler = method("feedClickHandler") {
        strings("click_media_option", "MediaOptionsOverflowHelper")
        returns(Type.Void)
        params(MEDIA_OPTION)
    }

    val reelsClickHandler = method("reelsClickHandler") {
        strings("instagram_clips_overflow_menu_option_tap", "Unsupported click action for Clips Viewer Overflow menu.")
        returns(Type.Void)
        params(MEDIA_OPTION)
    }

    val storyActionSheet = klass("storyActionSheet") {
        strings("archive_highlight_option", "copy_link_url", "delete_photo_title")
    }

    val storyLabelArray = method("storyLabelArray") {
        inClass(storyActionSheet)
        returns("java.lang.CharSequence[]")
    }

    val storyDispatchers = methods("storyDispatchers") {
        inClass(storyActionSheet)
        returns(Type.Void)
        hasParam(storyActionSheet.descriptor)
        hasParam(Type.CharSequence)
    }

    val media = bind("media") {
        fromField("feedMediaField") {
            owner(feedClickHandler.owner)
            nearestObjectReadBeforeString("click_media_option")
        }
        objectValue("dict") {
            field("dict") {
                rankBy("dict-like interface") { zeroArgListGetters() }
                requireScoreAtLeast(5)
            }
        }
        string("imageUrl") {
            field(EXTENDED_IMAGE_URL)
            callVirtual(EXTENDED_IMAGE_URL, "getUrl", "()Ljava/lang/String;")
        }
        string("videoUrl") {
            member("dict")
            listGetter("video_versions") {
                rankBy("callers followed by cast to VideoVersionIntf") { callSitesFollowedByCast(VIDEO_VERSION_INTF) }
            }
            first()
            cast(VIDEO_VERSION_INTF)
            callInterface(VIDEO_VERSION_INTF, "getUrl", "()Ljava/lang/String;")
        }
        objectValue("carouselChildren") {
            member("dict")
            listGetter("carousel_media") {
                rankBy("callers followed by cast to media type") { callSitesFollowedByCast(sourceType) }
            }
        }
    }

    val carouselIndexSetter = method("carouselIndexSetter") {
        strings("DirectShareSheetConstants.carousel_index")
        params(Type.Int)
    }

    val carouselIndexSetterCallers = methods("carouselIndexSetterCallers") {
        calls(carouselIndexSetter)
    }

    // The carousel state class is whatever holds the int read right before the setter is called.
    val carouselIndexField = fieldTarget("carouselIndexField") {
        carouselIndexSetterCallers.all.firstNotNullOfOrNull { caller ->
            val insns = caller.method.instructions
            val invoke = caller.method.indexOfFirstMethodCall(carouselIndexSetter.owner, carouselIndexSetter.name)
                ?: return@firstNotNullOfOrNull null
            val read = caller.method.indexOfFirstInstructionReversed(invoke - 1) { opcode == Opcode.IGET && fieldRef?.fieldType == Type.Int }
                ?: return@firstNotNullOfOrNull null
            insns[read].fieldRef
        } ?: error("No int field read precedes the carouselIndexSetter call in any caller")
    }

    val carouselStateClass = classTarget("carouselStateClass") {
        bytecode.findClass(carouselIndexField.owner) ?: error("carousel state class missing")
    }

    val carouselState = bind("carouselState") {
        fromClass(carouselStateClass)
        intValue("currentIndex") { field(carouselIndexField) }
    }

    val reelItemClass = klass(REEL_ITEM)

    val reelItemMediaField = fieldTarget("reelItemMediaField") {
        val mediaType = media.sourceType
        val candidates = reelItemClass.classDef.instanceFields.filter { it.fieldType == mediaType && AccessFlags.FINAL.isSet(it.accessFlags) }
        val field = candidates.singleOrNull()
            ?: error("Expected exactly one final $mediaType field on $REEL_ITEM, found ${candidates.size} (${candidates.joinToString { it.name }})")
        field.ref
    }

    val storyOwner = bind("storyOwner") {
        fromClass(storyActionSheet)
        objectValue("reelItem") { instanceField(REEL_ITEM) }
        context("context") { instanceField(listOf(Type.Activity, FRAGMENT_ACTIVITY, Type.Context)) }
        bind("media", media) {
            member("reelItem")
            field(reelItemMediaField)
        }
    }

    val reelItem = bind("reelItem") {
        fromClass(reelItemClass)
        bind("media", media) {
            field(reelItemMediaField)
        }
    }

    /** The label resource the feed menu passes for the DOWNLOAD option: the literal right after the enum load. */
    val feedDownloadLabel = feedMenuBuilder
        .point("feedDownloadLabel") { opcode(Opcode.SGET_OBJECT); field { owner(MEDIA_OPTION); name("DOWNLOAD") } }
        .next { where { this is app.reseam.patch.Instruction.RegLiteral } }

    val reelsLegacyMenuDisplay = method("reelsLegacyMenuDisplay") {
        calledBy(reelsClickHandler)
        returns(Type.Void)
        hasParam(Type.View)
    }

    val legacyMenu = bind("reelsLegacyMenu") {
        fromMethod(reelsLegacyMenuDisplay)
        raw { param(1) }
    }

    val legacyMenuClass = classTarget("reelsLegacyMenuClass") {
        bytecode.findClass(legacyMenu.sourceType) ?: error("legacy menu class missing")
    }

    val legacyMenuRow = method("reelsLegacyMenuRow") {
        inClass(legacyMenuClass)
        returns(Type.Void)
        params(Type.Context, "android.view.View\$OnClickListener", Type.String, Type.Int, Type.Boolean)
    }
}
