// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import app.reseam.bundle.postToMainThread
import java.util.concurrent.CopyOnWriteArraySet

internal fun interface CheckObserver {
    /** Called on the main thread. */
    fun onCheckChanged()
}

/** Tells the labels on screen that a check changed, so each one redraws. */
object CheckEvents {
    private val observers = CopyOnWriteArraySet<CheckObserver>()

    internal fun observe(observer: CheckObserver) = observers.add(observer)

    internal fun stopObserving(observer: CheckObserver) = observers.remove(observer)

    fun notifyChanged() {
        postToMainThread { observers.forEach(CheckObserver::onCheckChanged) }
    }
}
