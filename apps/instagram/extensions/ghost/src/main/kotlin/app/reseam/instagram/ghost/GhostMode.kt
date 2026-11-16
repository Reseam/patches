// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("GhostMode")

package app.reseam.instagram.ghost

import app.reseam.runtime.Export

// Set by Mark as read and taken by the receipt it sends, both on the main thread.
private var markReadRequested = false

@Export
fun allowMarkRead() {
    markReadRequested = true
}

@Export
fun takeMarkRead(): Boolean = markReadRequested.also { markReadRequested = false }
