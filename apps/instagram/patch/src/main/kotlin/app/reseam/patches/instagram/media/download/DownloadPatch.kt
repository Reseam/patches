// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.media.download

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.literal
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.DownloadSettings
import app.reseam.patches.instagram.core.FRAGMENT_ACTIVITY
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.MEDIA_OPTION
import app.reseam.patches.instagram.core.MediaDownloader
import app.reseam.patches.instagram.core.MediaMeta
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.internal.InstagramMediaGraph
import app.reseam.patches.instagram.internal.InstagramUserGraph

val downloadMedia = patch("Download media") {
    description("Adds a download option to feed, reels, and story overflow menus.")
    compatibleWith(INSTAGRAM)
    settings(instagramSettings, section("Downloads", DownloadSettings.folder, DownloadSettings.showToast))

    execute {
        installFeedMenuClick()
        installFeedMenuItem()
        installReelsMenuClick()
        installLegacyReelsMenu()
        installStoryMenu()
        implementMediaMeta()
    }
}

// The feed menu builder appends items right after casting the creator; the injected item goes
// before the branch that follows the static call, with the creator and list captured on the way.
val feedMenuInsert = InstagramMediaGraph.feedMenuBuilder
    .point("feedMenuInsert") { checkCast(InstagramMediaGraph.feedMenuCreator.descriptor) }
    .captureAs("creator")
    .previous { resultOf(Type.ArrayList) }
    .captureAs("menuList")
    .next { opcode(Opcode.INVOKE_STATIC_RANGE) }
    .next { opcode(Opcode.IF_EQZ) }

private fun PatchRuntime.installFeedMenuClick() {
    val graph = InstagramMediaGraph
    graph.feedClickHandler.before {
        val handler = thisObject
        val mediaValue = handler.field(graph.media.sourceField)
        val contextValue = handler.fieldOfType(FRAGMENT_ACTIVITY)
        val stateValue = handler.fieldOfType(graph.carouselStateClass.descriptor)
        val currentIndex = graph.carouselState.member("currentIndex", stateValue)
        val handled = call(MediaDownloader.handleFeedMenuClick, mediaValue, param(0), contextValue, currentIndex)
        whenTrue(handled) {
            returnVoid()
        }
    }
}

private fun PatchRuntime.installFeedMenuItem() {
    val graph = InstagramMediaGraph
    val label = graph.feedDownloadLabel.instruction.literal!!.toInt()
    feedMenuInsert.before {
        call(graph.feedMenuAddItem, enumValue(MEDIA_OPTION, "DOWNLOAD"), capture("creator"), capture("menuList"), int(label))
    }
}

private fun PatchRuntime.installReelsMenuClick() {
    val graph = InstagramMediaGraph
    graph.reelsClickHandler.before {
        whenEqual(param(0), enumValue(MEDIA_OPTION, "DOWNLOAD")) {
            val mediaValue = thisObject.fieldOfType(graph.media.sourceType)
            val contextValue = thisObject.fieldOfType(FRAGMENT_ACTIVITY)
            call(MediaDownloader.downloadMedia, mediaValue, contextValue)
            returnVoid()
        }
    }
}

private fun PatchRuntime.installLegacyReelsMenu() {
    val graph = InstagramMediaGraph
    graph.reelsLegacyMenuDisplay.before {
        val mediaValue = thisObject.fieldOfType(graph.media.sourceType)
        val contextValue = thisObject.fieldOfType(FRAGMENT_ACTIVITY)
        call(MediaDownloader.addLegacyDownloadRow, param(1), mediaValue, contextValue)
    }
}

private fun PatchRuntime.installStoryMenu() {
    val graph = InstagramMediaGraph
    graph.storyLabelArray.after {
        returnValue(call(MediaDownloader.appendStoryDownload, capture("result")))
    }
    graph.storyDispatchers.forEach {
        before {
            val matched = call(MediaDownloader.isStoryDownload, lastParam)
            whenTrue(matched) {
                call(MediaDownloader.downloadStory, paramOfType(graph.storyActionSheet.descriptor))
                returnVoid()
            }
        }
    }
}

private fun PatchRuntime.implementMediaMeta() {
    val graph = InstagramMediaGraph
    val users = InstagramUserGraph
    MediaMeta.username.implement {
        returnValue(users.principal.member("username", users.principalFromMedia.of(param(0))))
    }
    MediaMeta.reelItemMedia.implement {
        returnValue(graph.reelItem.member("media", param(0)))
    }
    MediaMeta.storyOwnerReelItem.implement {
        returnValue(graph.storyOwner.member("reelItem", param(0)))
    }
    MediaMeta.storyOwnerContext.implement {
        returnValue(graph.storyOwner.member("context", param(0)))
    }
    MediaMeta.addLegacyMenuRow.implement {
        val menu = graph.legacyMenu.of(param(0))
        menu.call(graph.legacyMenuRow, param(1), param(2), param(3), param(4), param(5))
        returnVoid()
    }
}
