// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("MainFolder")

package app.reseam.telegram.folders

import app.reseam.runtime.Export
import org.telegram.messenger.MessagesController
import org.telegram.tgnet.TLRPC

private const val ALL_CHATS = 0

/**
 * Moves All chats in the server's folder list to where it is on the phone. The server keeps it
 * first for an account without Premium, so every sync would undo the folder chosen as main.
 */
@Export
fun keepPosition(remote: ArrayList<TLRPC.DialogFilter>?, local: ArrayList<MessagesController.DialogFilter>?) {
    if (remote == null || local == null) return
    val position = local.indexOfFirst { it.id == ALL_CHATS }.takeIf { it >= 0 } ?: return
    val index = remote.indexOfFirst { it.id == ALL_CHATS }.takeIf { it >= 0 && it != position } ?: return
    remote.add(position.coerceAtMost(remote.size - 1), remote.removeAt(index))
}
