// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.refs

import app.reseam.patch.ExtMethod
import app.reseam.patch.MethodTarget
import app.reseam.patch.klass
import app.reseam.patch.patch
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.pinterest.refs.bindings.Pins

internal val pinRefs = patch {
    description("Reads pin, image and video values.")
    compatibleWith(PINTEREST)

    execute {
        val id = pinValue("id")
        val pin = id.parameterTypes.single()
        Pins.isPin.implement {
            whenInstanceOf(param(0), pin) { returnTrue() }
            returnFalse()
        }
        mapOf(
            Pins.id to id,
            Pins.title to pinValue("title"),
            Pins.promoted to pinValue("is_promoted"),
            Pins.shoppable to pinValue("is_shoppable"),
            Pins.createdAt to pinValue("created_at"),
            Pins.images to pinValue("images"),
            Pins.imageUrl to imageValue("url"),
            Pins.imageWidth to imageValue("width"),
            Pins.board to pinValue("board"),
            Pins.boardId to boardValue("id"),
            Pins.boardName to boardValue("name"),
            Pins.videoList to pinValue("videos"),
            Pins.videos to videoListValue("video_list"),
            Pins.videoUrl to videoValue("url"),
            Pins.videoBitrate to videoValue("bitrate"),
            Pins.story to pinValue("story_pin_data"),
            Pins.storyPages to storyValue("pages"),
            Pins.storyPagesPreview to storyValue("pages_preview"),
            Pins.storyBlocks to storyPageValue("blocks"),
            Pins.storyVideos to storyVideoValue("video_list"),
        ).forEach { (export, accessor) -> export.readsThrough(accessor) }

        val blockVideo = storyBlockValue("video")
        val videoBlock = blockVideo.parameterTypes.single()
        Pins.storyBlockVideo.implement {
            whenInstanceOf(param(0), storyBlock.descriptor) {
                val block = param(0).cast(storyBlock.descriptor).fieldOfType(videoBlock)
                whenNotNull(block) { returnValue(call(blockVideo, block)) }
            }
            returnNull()
        }
    }
}

// Lists such as a story page's blocks mix models, so a value of another type reads as null.
private fun ExtMethod.readsThrough(accessor: MethodTarget) = implement {
    val model = accessor.parameterTypes.single()
    whenInstanceOf(param(0), model) { returnValue(call(accessor, param(0).cast(model))) }
    returnNull()
}

private val storyBlock = klass("storyBlock") { hasInstanceField(storyBlockValue("video").parameterTypes.single()) }
