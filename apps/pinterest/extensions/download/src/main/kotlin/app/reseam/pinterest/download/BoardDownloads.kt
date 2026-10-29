// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("BoardDownloads")

package app.reseam.pinterest.download

import android.util.LruCache
import app.reseam.bundle.showToast
import app.reseam.pinterest.refs.board
import app.reseam.pinterest.refs.boardId
import app.reseam.pinterest.refs.id
import app.reseam.pinterest.refs.isPin
import app.reseam.pinterest.settings.PinterestStrings
import app.reseam.runtime.Export

private const val PINS_PER_BOARD = 500
private const val BOARDS = 20

private val loaded = LruCache<String, MutableMap<String, Any>>(BOARDS)

@Export
fun remember(models: List<Any?>) = models.forEach(::rememberPin)

@Export
fun rememberPin(pin: Any?) {
    if (!isPin(pin)) return
    val boardId = board(pin!!)?.let(::boardId) ?: return
    val pinId = id(pin) ?: return
    synchronized(loaded) {
        val pins = loaded[boardId] ?: LinkedHashMap<String, Any>().also { loaded.put(boardId, it) }
        if (pins.size < PINS_PER_BOARD) pins[pinId] = pin
    }
}

fun download(board: Any) {
    val pins = synchronized(loaded) { boardId(board)?.let(loaded::get)?.values?.toList() }.orEmpty()
    pins.forEach(::enqueue)
    app.showToast(if (pins.isEmpty()) PinterestStrings.downloadBoardEmpty else PinterestStrings.downloadBoardStarted(pins.size))
}

@Export fun closeMenu(screen: Any) {}

@Export fun withItem(items: List<Any?>, item: Any): List<Any?> = items + item

@Export
fun menuAction(menu: (Any?) -> Any?, index: Int, board: Any, screen: Any): (Any?) -> Any? = { option ->
    if (option == index) {
        closeMenu(screen)
        download(board)
        // The sheet ignores what its action handler returns.
        null
    } else {
        menu(option)
    }
}
