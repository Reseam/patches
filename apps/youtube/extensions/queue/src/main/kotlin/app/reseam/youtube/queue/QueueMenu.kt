// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("QueueMenu")

package app.reseam.youtube.queue

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.Endpoints
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.debug

private val menuItems = intArrayOf(98150882, 1, 66439850, 1, 77258115)
private const val ITEM_ICON = 2
private const val ITEM_COMMAND = 3
private const val ITEM_BADGE = 12
private const val ICON_TYPE = 1
private const val ICON_QUEUE_PLAY_NEXT = 251L
private const val CAST_PLAY_NEXT = 122330696

@Export
fun unlock(command: ByteArray): ByteArray? {
    try {
        val videoId = menuVideoId(command) ?: return null
        val unlocked = rewrite(command, 0) { if (isUpsell(it)) queueItem(it, videoId) else it }
        return unlocked.takeUnless { it === command }
    } catch (exception: IllegalArgumentException) {
        Logger.debug { "Menu is not a queue menu" }
        return null
    }
}

// Newer menus name their video in the cast item; older ones in the queue items that already hold the
// queue command, beside the upsell shown while nothing plays.
private fun menuVideoId(command: ByteArray): String? {
    val commands = menuEntries(command).mapNotNull { Proto.bytes(it, ITEM_COMMAND) }
    // Stop at the first cast command even when it has no video ID.
    commands.firstNotNullOfOrNull { Proto.bytes(it, CAST_PLAY_NEXT) }?.let { return Proto.string(it, 1) }
    return commands.firstNotNullOfOrNull { Proto.bytes(it, PLAYER_STATE_COMMAND)?.let(::playerStateVideoId) }
}

private fun isUpsell(item: ByteArray): Boolean {
    val icon = Proto.bytes(item, ITEM_ICON)
    val command = Proto.bytes(item, ITEM_COMMAND)
    if (icon == null || command == null) return false
    val queueIcon = Proto.number(icon, ICON_TYPE, 0) == ICON_QUEUE_PLAY_NEXT
    return queueIcon && Proto.bytes(command, Endpoints.BROWSE) != null
}

private fun queueItem(item: ByteArray, videoId: String): ByteArray {
    val command =
        Proto.replace(
            checkNotNull(Proto.bytes(item, ITEM_COMMAND)) { "Queue item has no command" },
            mapOf(
                Endpoints.BROWSE to null,
                PLAYER_STATE_COMMAND to playerStateCommand(videoId, POSITION_NEXT),
            ),
        )
    return Proto.replace(item, mapOf(ITEM_COMMAND to command, ITEM_BADGE to null))
}

private fun menuEntries(message: ByteArray, depth: Int = 0): List<ByteArray> =
    if (depth == menuItems.size) listOf(message)
    else
        Proto.fields(message)
            .filter { it.number == menuItems[depth] && it.wireType == Proto.WIRE_LENGTH }
            .flatMap {
                menuEntries(
                    checkNotNull(it.bytes) { "Length-delimited field has no bytes" },
                    depth + 1,
                )
            }

private fun rewrite(message: ByteArray, depth: Int, item: (ByteArray) -> ByteArray): ByteArray =
    if (depth == menuItems.size) item(message)
    else
        Proto.rewriteLength(message) { number, value ->
            if (number == menuItems[depth]) rewrite(value, depth + 1, item) else value
        }
