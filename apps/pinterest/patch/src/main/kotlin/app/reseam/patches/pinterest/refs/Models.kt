// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.refs

import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.point

// Pinterest's JSON adapters write each key followed by the static accessor that holds its value.
private fun MethodTarget.valueOf(key: String): MethodTarget =
    point { string(key); then(within = 3) { invokeStatic { paramCount(1) } } }.callee("$this.$key")

private fun modelWriter(debugName: String, vararg keys: String) = method(debugName) {
    strings(*keys)
    returns(Type.Void)
    paramCount(2)
}

private val pinWriter = modelWriter("pinWriter", "is_promoted", "is_shoppable", "created_at", "images")
private val imageWriter = modelWriter("imageWriter", "dominant_color", "url", "width", "height")
private val videoListWriter = modelWriter("videoListWriter", "video_list", "signature", "duration")
private val videoWriter = modelWriter("videoWriter", "url", "width", "height", "duration", "thumbnail")
private val boardWriter = modelWriter("boardWriter", "pin_count", "collaborated_by_me", "section_count")
private val storyWriter = modelWriter("storyWriter", "pages", "page_count", "metadata")
private val storyPageWriter = modelWriter("storyPageWriter", "blocks", "style", "layout")
private val storyBlockWriter = modelWriter("storyBlockWriter", "video", "block_type", "block_style")
private val storyVideoWriter = modelWriter("storyVideoWriter", "bitrates", "video_list")

fun pinValue(key: String) = pinWriter.valueOf(key)
fun imageValue(key: String) = imageWriter.valueOf(key)
fun videoListValue(key: String) = videoListWriter.valueOf(key)
fun videoValue(key: String) = videoWriter.valueOf(key)
fun boardValue(key: String) = boardWriter.valueOf(key)
fun storyValue(key: String) = storyWriter.valueOf(key)
fun storyPageValue(key: String) = storyPageWriter.valueOf(key)
fun storyBlockValue(key: String) = storyBlockWriter.valueOf(key)
fun storyVideoValue(key: String) = storyVideoWriter.valueOf(key)
