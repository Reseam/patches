// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

inline fun <T, R> T.useReleasing(release: T.() -> Unit, action: (T) -> R): R =
    try {
        action(this)
    } finally {
        release()
    }
