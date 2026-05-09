// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.internal

import app.reseam.patch.AccessFlags
import app.reseam.patch.Binding
import app.reseam.patch.ClassHandle
import app.reseam.patch.FieldHandle
import app.reseam.patch.FieldRef
import app.reseam.patch.MethodHandle
import app.reseam.patch.Opcodes
import app.reseam.patch.PatchContext
import app.reseam.patch.bind
import app.reseam.patch.classHandle
import app.reseam.patch.fieldRef
import app.reseam.patch.opcode
import app.reseam.patch.findClass
import app.reseam.patch.findMethod
import app.reseam.patch.findMethods
import app.reseam.patch.indexOfFirstFieldAccess
import app.reseam.patch.wrapField
import app.reseam.patches.instagram.media.download.ACTIVITY_TYPE
import app.reseam.patches.instagram.media.download.CONTEXT_TYPE
import app.reseam.patches.instagram.media.download.FRAGMENT_ACTIVITY_TYPE
import app.reseam.patches.instagram.media.download.MEDIA_OPTION_TYPE
import app.reseam.patches.instagram.media.download.REEL_ITEM_TYPE

private const val EXTENDED_IMAGE_URL_TYPE = "Lcom/instagram/model/mediasize/ExtendedImageUrl;"
private const val VIDEO_VERSION_INTF_TYPE = "Lcom/instagram/model/mediasize/VideoVersionIntf;"

internal interface RuntimeMedia
internal interface RuntimeFeedListener
internal interface RuntimeStoryOwner
internal interface RuntimeReelItem
internal interface RuntimeLegacyMenu
internal interface RuntimeCarouselState

internal class InstagramMediaGraph(private val ctx: PatchContext) {
    val feedMenuBuilder: MethodHandle = ctx.findMethod(debug = "feedMenuBuilder") {
        strings("instagram_feed_self_view_overflow_menu_insights_option_impression")
        returnType("Ljava/lang/Object;")
    }

    val feedMenuCreator: ClassHandle = ctx.findClass(debug = "feedMenuCreator") {
        strings("MediaOptionsOverflowMenuCreator")
    }

    val feedMenuAddItem: MethodHandle = ctx.findMethod(debug = "feedMenuAddItem") {
        inClass(feedMenuCreator)
        returnType("V")
        parameterTypes(MEDIA_OPTION_TYPE, feedMenuCreator.descriptor, "Ljava/util/ArrayList;", "I")
    }

    val feedClickHandler: MethodHandle = ctx.findMethod(debug = "feedClickHandler") {
        strings("click_media_option", "MediaOptionsOverflowHelper")
        returnType("V")
        parameterTypes(MEDIA_OPTION_TYPE)
    }

    val feedClickListener: MethodHandle = ctx.findMethod(debug = "feedClickListener") {
        returnType("V")
        parameterTypes("Landroid/view/View;")
        calls(feedClickHandler)
    }

    val reelsClickHandler: MethodHandle = ctx.findMethod(debug = "reelsClickHandler") {
        strings(
            "instagram_clips_overflow_menu_option_tap",
            "Unsupported click action for Clips Viewer Overflow menu.",
        )
        returnType("V")
        parameterTypes(MEDIA_OPTION_TYPE)
    }

    val storyActionSheet: ClassHandle = ctx.findClass(debug = "storyActionSheet") {
        strings("archive_highlight_option", "copy_link_url", "delete_photo_title")
    }

    val storyLabelArray: MethodHandle = ctx.findMethod(debug = "storyLabelArray") {
        inClass(storyActionSheet)
        returnType("[Ljava/lang/CharSequence;")
    }

    val storyDispatchers: List<MethodHandle> = ctx.findMethods(debug = "storyDispatchers") {
        inClass(storyActionSheet)
        returnType("V")
        hasParameter(storyActionSheet.descriptor)
        hasParameter("Ljava/lang/CharSequence;")
    }

    val media: Binding<RuntimeMedia> = ctx.bind(debug = "media") {
        fromField(debug = "feedMediaField") {
            owner(feedClickHandler.classDescriptor)
            nearestObjectReadBeforeString("click_media_option")
        }

        objectValue("dict") {
            field("dict") {
                rankBy("dict-like interface") {
                    type().zeroArgListGetters()
                }
                requireScoreAtLeast(5)
            }
        }

        string("imageUrl") {
            field(type = EXTENDED_IMAGE_URL_TYPE)
            callVirtual(EXTENDED_IMAGE_URL_TYPE, "getUrl", "()Ljava/lang/String;")
        }

        string("videoUrl") {
            member("dict")
            listGetter("video_versions") {
                rankBy("callers followed by cast to VideoVersionIntf") {
                    callSites().followedByCheckCast(VIDEO_VERSION_INTF_TYPE, 40).count()
                }
            }
            first()
            cast(VIDEO_VERSION_INTF_TYPE)
            callInterface(VIDEO_VERSION_INTF_TYPE, "getUrl", "()Ljava/lang/String;")
        }

        objectValue("carouselChildren") {
            member("dict")
            listGetter("carousel_media") {
                rankBy("callers followed by cast to media type") {
                    callSites().followedByCheckCast(sourceType, 40).count()
                }
            }
        }
    }

    val extractMediaClickId: MethodHandle = ctx.findMethod(debug = "extractMediaClickId") {
        strings(
            "extractMediaClickId: currentCarouselPosition out of bounds, positionFromState: ",
            ", carouselSize: ",
        )
    }

    val carouselStateClass: ClassHandle = run {
        val method = extractMediaClickId.method
        val idx = method.indexOfFirstFieldAccess(Opcodes.IGET, fieldType = "I")
            ?: error("No IGET 'I' instruction in extractMediaClickId — carousel state class anchor is gone.")
        val ref = method.instructions[idx].fieldRef()
            ?: error("Instruction at index $idx in extractMediaClickId has no field reference.")
        ctx.classHandle(ref.definingClass, debug = "carouselStateClass")
    }

    val carouselIndexSetter: MethodHandle = ctx.findMethod(debug = "carouselIndexSetter") {
        strings("DirectShareSheetConstants.carousel_index")
    }

    val carouselIndexField: FieldHandle = run {
        val callers = ctx.findMethods(debug = "carouselIndexSetterCallers") {
            calls(carouselIndexSetter)
        }
        require(callers.isNotEmpty()) {
            "No callers of carouselIndexSetter (LX/MvR.A02) — cannot anchor carousel-position field."
        }
        val stateDescriptor = carouselStateClass.descriptor
        var found: FieldRef? = null
        outer@ for (caller in callers) {
            val insns = caller.method.instructions
            for (i in insns.indices) {
                val cur = insns[i]
                if (cur.opcode() != Opcodes.IGET) continue
                val ref = cur.fieldRef() ?: continue
                if (ref.definingClass != stateDescriptor) continue
                if (ref.fieldType != "I") continue
                found = ref
                break@outer
            }
        }
        val ref = found
            ?: error("No IGET 'I' on $stateDescriptor in any caller of carouselIndexSetter.")
        ctx.wrapField(ref, debug = "carouselIndexField")
    }

    val carouselState: Binding<RuntimeCarouselState> = ctx.bind(debug = "carouselState") {
        fromClass(carouselStateClass)

        intValue("currentIndex") {
            field(carouselIndexField)
        }
    }

    val feedListener: Binding<RuntimeFeedListener> = ctx.bind(debug = "feedListener") {
        fromMethod(debug = "feedOverflowClickListener") {
            sameAs(feedClickListener)
        }

        objectValue("clickedOption") {
            objectSlots().firstInstanceOf(MEDIA_OPTION_TYPE)
        }

        bind("media", media) {
            objectSlots().firstInstanceOf(feedClickHandler.classDescriptor)
            field(media.sourceField())
            asBinding(media)
        }

        bind("carouselState", carouselState) {
            objectSlots().firstInstanceOf(feedClickHandler.classDescriptor)
            instanceField(type = carouselStateClass.descriptor)
            asBinding(carouselState)
        }
    }

    val reelItemMediaField: FieldHandle = run {
        val mediaType = media.sourceType
        val reelClass = ctx.classHandle(REEL_ITEM_TYPE, debug = "reelItem").classDef
        val finalCandidates = reelClass.instanceFields.filter {
            it.fieldType == mediaType && (it.accessFlags.toInt() and AccessFlags.FINAL) != 0
        }
        val info = finalCandidates.singleOrNull()
            ?: error(
                "Expected exactly one final ${mediaType} field on ${REEL_ITEM_TYPE}, " +
                    "found ${finalCandidates.size} (${finalCandidates.joinToString { it.name }})",
            )
        ctx.wrapField(FieldRef(REEL_ITEM_TYPE, info.name, info.fieldType), debug = "reelItemMediaField")
    }

    val storyOwner: Binding<RuntimeStoryOwner> = ctx.bind(debug = "storyOwner") {
        fromClass(storyActionSheet)

        objectValue("reelItem") {
            instanceField(type = REEL_ITEM_TYPE)
        }

        context("context") {
            instanceField(typeAnyOf = listOf(ACTIVITY_TYPE, FRAGMENT_ACTIVITY_TYPE, CONTEXT_TYPE))
        }

        bind("media", media) {
            member("reelItem")
            field(reelItemMediaField)
            asBinding(media)
        }
    }

    val reelItem: Binding<RuntimeReelItem> = ctx.bind(debug = "reelItem") {
        fromClass(ctx.classHandle(REEL_ITEM_TYPE, debug = "reelItem"))

        bind("media", media) {
            field(reelItemMediaField)
            asBinding(media)
        }
    }

    fun feedDownloadLabel(): Int =
        feedMenuBuilder.constantAfterEnum(MEDIA_OPTION_TYPE, "DOWNLOAD")

    val reelsLegacyMenuDisplay: MethodHandle = ctx.findMethod(debug = "reelsLegacyMenuDisplay") {
        calledBy(reelsClickHandler)
        returnType("V")
        hasParameter("Landroid/view/View;")
    }

    val legacyMenu: Binding<RuntimeLegacyMenu> = ctx.bind(debug = "reelsLegacyMenu") {
        fromMethod(debug = "reelsLegacyMenuDisplay") {
            sameAs(reelsLegacyMenuDisplay)
        }

        raw {
            parameter(1)
        }
    }

    val legacyMenuRow: MethodHandle = ctx.findMethod(debug = "reelsLegacyMenuRow") {
        inClass(ctx.classHandle(legacyMenu.sourceType, debug = "reelsLegacyMenuClass"))
        returnType("V")
        parameterTypes(
            CONTEXT_TYPE,
            "Landroid/view/View\$OnClickListener;",
            "Ljava/lang/String;",
            "I",
            "Z",
        )
    }
}
