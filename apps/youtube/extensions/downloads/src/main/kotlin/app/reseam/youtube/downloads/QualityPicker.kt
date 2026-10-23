// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("QualityPicker")

package app.reseam.youtube.downloads

import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.YouTubeContext

private const val RESPONSE_COMMAND = 2
private const val GET_DOWNLOAD_ACTION = 382320942
private const val UPSELL_DIALOG_MODEL = 39181032
private const val OFFLINE_VIDEO_ENDPOINT = 73080600
private const val OFFLINEABILITY = 60572968
private const val ACTION_ADD = 1L
private const val AVAILABILITY_FREE = 1L

private enum class Quality(val value: Long, val label: String) {
    FULL_HD(7, "offline_video_quality_1080p"),
    HD(2, "offline_video_quality_720p"),
    SD(1, "offline_video_quality_360p"),
    LOW(4, "offline_video_quality_144p"),
}

@Volatile private var requestedVideoId: String? = null

@Export
fun requested(command: ByteArray) {
    val action = Proto.bytes(command, GET_DOWNLOAD_ACTION) ?: return
    Proto.string(action, 1)?.let { requestedVideoId = it }
}

internal fun replaceUpsell(response: ByteArray): ByteArray? {
    val command = Proto.bytes(response, RESPONSE_COMMAND)
    val videoId = requestedVideoId
    if (command == null || videoId == null || !containsUpsell(command)) return null
    return Proto.replace(response, mapOf(RESPONSE_COMMAND to addCommand(videoId)))
}

private fun addCommand(videoId: String): ByteArray =
    Proto.Writer()
        .message(
            OFFLINE_VIDEO_ENDPOINT,
            Proto.Writer().string(1, videoId).number(2, ACTION_ADD).bytes(4, unlockedOfflineability()),
        )
        .toByteArray()

/** Offlineability offering every download quality to any account, as a renderer the endpoint carries. */
internal fun unlockedOfflineability(): ByteArray {
    val offlineability = Proto.Writer().number(1, 1)
    val context = YouTubeContext.applicationContext
    Quality.entries.forEach { quality ->
        val label = context.identifier(quality.label, "string")
        offlineability.message(
            3,
            Proto.Writer()
                .message(1, Proto.Writer().string(4, context.getString(label)))
                .number(3, quality.value)
                .number(4, AVAILABILITY_FREE),
        )
    }
    return Proto.Writer().message(OFFLINEABILITY, offlineability).toByteArray()
}

private fun containsUpsell(message: ByteArray): Boolean =
    try {
        Proto.fields(message).any { field ->
            when {
                field.wireType != Proto.WIRE_LENGTH -> false
                field.number == UPSELL_DIALOG_MODEL -> true
                else -> containsUpsell(checkNotNull(field.bytes))
            }
        }
    } catch (_: IllegalArgumentException) {
        false
    }
