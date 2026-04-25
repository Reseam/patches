// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.internal

import app.reseam.patch.Binding
import app.reseam.patch.ClassHandle
import app.reseam.patch.MethodHandle
import app.reseam.patch.PatchContext
import app.reseam.patch.bind
import app.reseam.patch.classHandle
import app.reseam.patch.findClass
import app.reseam.patch.findMethod
import app.reseam.patch.findMethods
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
            field("reelItemMedia") {
                rankBy("list-bearing media field") {
                    type().methods(proto = "()Ljava/util/List;").size
                }
            }
            asBinding(media)
        }
    }

    val reelItem: Binding<RuntimeReelItem> = ctx.bind(debug = "reelItem") {
        fromClass(ctx.classHandle(REEL_ITEM_TYPE, debug = "reelItem"))

        bind("media", media) {
            field("reelItemMedia") {
                rankBy("list-bearing media field") {
                    type().methods(proto = "()Ljava/util/List;").size
                }
            }
            asBinding(media)
        }
    }

    fun feedDownloadLabel(): Int =
        feedMenuBuilder.constantAfterEnum(MEDIA_OPTION_TYPE, "DOWNLOAD")

    val reelsLegacyMenuDisplay: MethodHandle = ctx.findMethod(debug = "reelsLegacyMenuDisplay") {
        calledBy(reelsClickHandler)
        returnType("V")
        parameterTypes("Landroid/view/View;", "Ljava/lang/Object;")
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
