// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.navigation

import app.reseam.bundle.Logger
import app.reseam.bundle.isMainThread
import app.reseam.youtube.core.debug
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val LATCH_TIMEOUT_MILLISECONDS = 120L

/** Waiters retain their selection generation, so a timeout cannot release its replacement. */
internal class NavigationSelectionLatch(private val monitor: Any) {
    @Volatile private var pending: CountDownLatch? = null

    fun reset() =
        synchronized(monitor) {
            pending?.countDown()
            pending = CountDownLatch(1)
        }

    fun release() = release(pending)

    private fun release(latch: CountDownLatch?) {
        synchronized(monitor) {
            if (latch != null && latch === pending) {
                pending = null
                latch.countDown()
            }
        }
    }

    fun awaitSelection() {
        val latch = pending ?: return
        if (isMainThread()) return
        try {
            if (!latch.await(LATCH_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)) {
                release(latch)
                Logger.debug { "Navigation tab update wait timed out" }
            }
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            Logger.error(exception) { "Navigation tab update wait interrupted" }
        }
    }
}
