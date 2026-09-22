// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.media.download

import app.reseam.patch.ExtClass
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.DownloadSettings
import app.reseam.patches.instagram.core.FRAGMENT_ACTIVITY
import app.reseam.patches.instagram.core.FeedMenuOption
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.MEDIA_OPTION
import app.reseam.patches.instagram.core.MediaDownloader
import app.reseam.patches.instagram.core.MediaMeta
import app.reseam.patches.instagram.core.UserRefs
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.internal.InstagramMediaGraph
import app.reseam.patches.instagram.refs.mediaRefs
import app.reseam.patches.instagram.refs.userRefs

private const val MEDIA = "com.instagram.feed.media.Media"

private object DownloadOption : ExtClass("app.reseam.instagram.download.DownloadOption") {
    val isDownload = static("isDownload", Type.Object, returns = Type.Boolean)
}

val downloadMedia = patch("Download media") {
    description("Adds a download option to feed, reels, and story overflow menus.")
    compatibleWith(INSTAGRAM)
    dependsOn(mediaRefs)
    dependsOn(userRefs)
    settings(instagramSettings, section("Downloads", DownloadSettings.folder, DownloadSettings.showToast))

    execute {
        installFeedMenuClick()
        installFeedMenuItem()
        installFeedSimplifiedMenu()
        installReelsMenuItem()
        installReelsMenuClick()
        installStoryMenu()
        implementMediaMeta()
        FeedMenuOption.option.implement {
            returnValue(param(0).cast(InstagramMediaGraph.feedMenuRow.descriptor).fieldOfType(MEDIA_OPTION))
        }
        FeedMenuOption.setLabel.implement {
            param(0).cast(InstagramMediaGraph.feedMenuRow.descriptor).set(InstagramMediaGraph.feedMenuRowLabel, param(1))
            returnVoid()
        }
        DownloadOption.isDownload.implement {
            whenEqual(param(0), enumValue(MEDIA_OPTION, "DOWNLOAD")) { returnTrue() }
            returnFalse()
        }
    }
}

private fun PatchRuntime.installFeedMenuClick() {
    val graph = InstagramMediaGraph
    graph.feedClickHandler.before {
        val handler = thisObject
        val mediaValue = call(graph.feedMediaGetter, handler)
        val contextValue = handler.fieldOfType(FRAGMENT_ACTIVITY)
        val currentIndex = handler.field(graph.feedCarouselIndex)
        val handled = call(MediaDownloader.handleFeedMenuClick, mediaValue, param(0), contextValue, currentIndex)
        whenTrue(handled) {
            returnVoid()
        }
    }
}

private fun PatchRuntime.installFeedMenuItem() {
    val graph = InstagramMediaGraph
    val label = resources.addString("reseam_download", "Download")?.toInt()
        ?: error("Could not add download label")
    graph.feedMenuAppendRow.after {
        val option = enumValue(MEDIA_OPTION, "DOWNLOAD")
        whenFalse(call(MediaDownloader.hasDownloadOption, param(4), option)) {
            call(graph.feedMenuAddItem, option, param(2), param(4), int(label))
        }
        call(MediaDownloader.labelDownloadOption, param(4), option)
    }
}

private fun PatchRuntime.installFeedSimplifiedMenu() {
    InstagramMediaGraph.feedSimplifiedAllowedOptions.after {
        returnValue(call(MediaDownloader.withDownloadOption, capture("result"), enumValue(MEDIA_OPTION, "DOWNLOAD")))
    }
}

private fun PatchRuntime.installReelsMenuClick() {
    val graph = InstagramMediaGraph
    graph.reelsClickHandler.before {
        whenEqual(param(0), enumValue(MEDIA_OPTION, "DOWNLOAD")) {
            val mediaValue = thisObject.fieldOfType(MEDIA)
            val contextValue = thisObject.fieldOfType(FRAGMENT_ACTIVITY)
            call(MediaDownloader.downloadMedia, mediaValue, contextValue)
            returnVoid()
        }
    }
}

private fun PatchRuntime.installReelsMenuItem() {
    val row = InstagramMediaGraph.reelsRowAdder
    row.after {
        val menu = param(2)
        whenTrue(call(MediaDownloader.claimReelsMenu, menu)) {
            thisObject.call(row, param(0), enumValue(MEDIA_OPTION, "DOWNLOAD"), menu, param(3))
        }
    }
}

private fun PatchRuntime.installStoryMenu() {
    val graph = InstagramMediaGraph
    graph.storyLabelArrays.forEach {
        after { returnValue(call(MediaDownloader.appendStoryDownload, capture("result"))) }
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
    MediaMeta.username.implement {
        whenNotNull(param(0)) {
            returnValue(call(UserRefs.username, call(UserRefs.fromMedia, param(0))))
        }
        returnNull()
    }
    MediaMeta.reelItemMedia.implement {
        returnValue(param(0).cast(graph.reelItemClass.descriptor).field(graph.reelItemMediaField))
    }
    MediaMeta.storyOwnerReelItem.implement {
        returnValue(param(0).cast(graph.storyActionSheet.descriptor).fieldOfType("com.instagram.model.reels.ReelItem"))
    }
    MediaMeta.storyOwnerContext.implement {
        returnValue(param(0).cast(graph.storyActionSheet.descriptor).fieldOfType(Type.Activity))
    }
}
