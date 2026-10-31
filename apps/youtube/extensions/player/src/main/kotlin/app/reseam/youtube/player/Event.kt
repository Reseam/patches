// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.player

import java.util.concurrent.CopyOnWriteArraySet

class Event<T> {
    private val observers = CopyOnWriteArraySet<(T) -> Unit>()

    fun add(observer: (T) -> Unit) {
        observers.add(observer)
    }

    fun remove(observer: (T) -> Unit) {
        observers.remove(observer)
    }

    fun fire(value: T) {
        observers.forEach { it(value) }
    }
}
