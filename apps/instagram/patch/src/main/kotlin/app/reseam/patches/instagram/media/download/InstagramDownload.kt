// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.media.download

import app.reseam.patch.PatchContext
import app.reseam.patch.bindStub
import app.reseam.patches.instagram.internal.InstagramMediaGraph

private const val MEDIA_META = "Lapp/reseam/instagram/download/MediaMeta;"

internal class InstagramDownload(private val ctx: PatchContext) {
    private val graph = InstagramMediaGraph(ctx)

    fun install() {
        installFeedMenuClick()
        installFeedMenuItems()
        installReelsMenuClick()
        installLegacyReelsMenu()
        installStoryMenu()
        bindMediaMetaStubs()
    }

    private fun installFeedMenuClick() {
        graph.feedClickListener.before {
            val listener = thisObject()
            val handled = staticCall(EXT, "handleFeedMenuClick", "(Ljava/lang/Object;)Z", listener)
            ifTrue(handled) {
                returnVoid()
            }
        }
        ctx.log.info("Hooked feed menu click handler")
    }

    private fun installFeedMenuItems() {
        val labelResId = graph.feedDownloadLabel()

        graph.feedMenuBuilder.point(debug = "feedMenuInsert") {
            checkCast(graph.feedMenuCreator.descriptor)
            previousResult("Ljava/util/ArrayList;")
            nextBranch(opcode = "IF_EQZ", afterInvokeOpcode = "INVOKE_STATIC_RANGE")
        }.insertBefore {
            val creator = capture("creator", graph.feedMenuCreator.descriptor)
            val list = capture("menuList", "Ljava/util/ArrayList;")
            staticCall(
                graph.feedMenuAddItem.classDescriptor,
                graph.feedMenuAddItem.methodName,
                graph.feedMenuAddItem.proto,
                enumObject(MEDIA_OPTION_TYPE, "DOWNLOAD"),
                creator,
                list,
                int(labelResId),
            )
        }

        ctx.log.info("Injected feed download menu item")
    }

    private fun installReelsMenuClick() {
        graph.reelsClickHandler.before {
            val option = parameter(0)
            ifEqual(option, enumObject(MEDIA_OPTION_TYPE, "DOWNLOAD")) {
                val mediaValue = thisObject().field(type = graph.media.sourceType)
                val contextValue = thisObject().field(type = FRAGMENT_ACTIVITY_TYPE)
                staticCall(
                    EXT,
                    "downloadMedia",
                    "(Ljava/lang/Object;Landroid/content/Context;)V",
                    mediaValue,
                    contextValue,
                )
                returnVoid()
            }
        }
        ctx.log.info("Hooked reels menu click")
    }

    private fun installLegacyReelsMenu() {
        graph.reelsLegacyMenuDisplay.before {
            val menu = parameter(1)
            val mediaValue = thisObject().field(type = graph.media.sourceType)
            val contextValue = thisObject().field(type = FRAGMENT_ACTIVITY_TYPE)
            staticCall(
                EXT,
                "addLegacyDownloadRow",
                "(Ljava/lang/Object;Ljava/lang/Object;Landroid/content/Context;)V",
                menu,
                mediaValue,
                contextValue,
            )
        }
        ctx.log.info("Hooked legacy reels menu")
    }

    private fun installStoryMenu() {
        graph.storyLabelArray.after {
            val items = capture("storyItems", "[Ljava/lang/CharSequence;")
            val updated = staticCall(
                EXT,
                "appendStoryDownload",
                "([Ljava/lang/CharSequence;)[Ljava/lang/CharSequence;",
                items,
            )
            returnObject(updated)
        }

        graph.storyDispatchers.forEach { method ->
            method.before {
                val label = parameterLast()
                val matched = staticCall(EXT, "isStoryDownload", "(Ljava/lang/CharSequence;)Z", label)
                ifTrue(matched) {
                    val owner = parameterOfType(graph.storyActionSheet.descriptor)
                    staticCall(EXT, "downloadStory", "(Ljava/lang/Object;)V", owner)
                    returnVoid()
                }
            }
        }

        ctx.log.info("Hooked story download menu")
    }

    private fun bindMediaMetaStubs() {
        ctx.bindStub(MEDIA_META) {
            method("username", "(Ljava/lang/Object;)Ljava/lang/String;") {
                returnObject(string(""))
            }

            method("videoUrl", "(Ljava/lang/Object;)Ljava/lang/String;") {
                returnObject(graph.media.member("videoUrl", parameter(0)))
            }

            method("imageUrl", "(Ljava/lang/Object;)Ljava/lang/String;") {
                returnObject(graph.media.member("imageUrl", parameter(0)))
            }

            method("carouselChildren", "(Ljava/lang/Object;)Ljava/util/List;") {
                returnObject(graph.media.member("carouselChildren", parameter(0)))
            }

            method("listenerMedia", "(Ljava/lang/Object;)Ljava/lang/Object;") {
                returnObject(graph.feedListener.member("media", parameter(0)))
            }

            method("listenerDownloadOption", "(Ljava/lang/Object;)Ljava/lang/Object;") {
                returnObject(graph.feedListener.member("clickedOption", parameter(0)))
            }

            method("reelItemMedia", "(Ljava/lang/Object;)Ljava/lang/Object;") {
                returnObject(graph.reelItem.member("media", parameter(0)))
            }

            method("storyOwnerReelItem", "(Ljava/lang/Object;)Ljava/lang/Object;") {
                returnObject(graph.storyOwner.member("reelItem", parameter(0)))
            }

            method("storyOwnerContext", "(Ljava/lang/Object;)Ljava/lang/Object;") {
                returnObject(graph.storyOwner.member("context", parameter(0)))
            }

            method("addLegacyMenuRow", "(Ljava/lang/Object;Landroid/content/Context;Landroid/view/View\$OnClickListener;Ljava/lang/String;IZ)V") {
                val menu = graph.legacyMenu.of(parameter(0))
                menu.virtualCall(
                    graph.legacyMenuRow.classDescriptor,
                    graph.legacyMenuRow.methodName,
                    graph.legacyMenuRow.proto,
                    parameter(1),
                    parameter(2),
                    parameter(3),
                    parameter(4),
                    parameter(5),
                )
                returnVoid()
            }
        }

        ctx.log.info("Bound MediaMeta bridges through bindings and stub compiler")
    }
}
