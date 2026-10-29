// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Pins")

package app.reseam.pinterest.refs

import app.reseam.runtime.Export
import java.util.Date

@Export fun isPin(item: Any?): Boolean = false

@Export fun id(pin: Any): String? = null

@Export fun title(pin: Any): String? = null

@Export fun promoted(pin: Any): Boolean? = null

@Export fun shoppable(pin: Any): Boolean? = null

@Export fun createdAt(pin: Any): Date? = null

@Export fun images(pin: Any): Map<String, Any>? = null

@Export fun imageUrl(image: Any): String? = null

@Export fun imageWidth(image: Any): Double? = null

@Export fun board(pin: Any): Any? = null

@Export fun boardId(board: Any): String? = null

@Export fun boardName(board: Any): String? = null

@Export fun videoList(pin: Any): Any? = null

@Export fun videos(videoList: Any): Map<String, Any>? = null

@Export fun videoUrl(video: Any): String? = null

@Export fun videoBitrate(video: Any): Number? = null

@Export fun story(pin: Any): Any? = null

@Export fun storyPages(story: Any): List<Any>? = null

@Export fun storyPagesPreview(story: Any): List<Any>? = null

@Export fun storyBlocks(page: Any): List<Any>? = null

@Export fun storyBlockVideo(block: Any): Any? = null

@Export fun storyVideos(video: Any): Map<String, Any>? = null
