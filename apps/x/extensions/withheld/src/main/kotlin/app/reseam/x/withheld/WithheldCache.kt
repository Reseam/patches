// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.withheld

import android.os.SystemClock
import app.reseam.bundle.Logger
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

// X holds the screen until its response returns, so lookups get a fixed budget. A lookup that
// finishes later still lands in the cache and shows on the next load.
internal object WithheldCache {
    private const val WAIT_MS = 4_000L
    private const val FOUND_TTL_MS = 30 * 60_000L
    private const val MISSING_TTL_MS = 5 * 60_000L
    private const val MAX_ENTRIES = 500

    private val network =
        Executors.newFixedThreadPool(4) { runnable -> Thread(runnable, "Reseam-Withheld").apply { isDaemon = true } }

    val statuses = Lookup("status", FxTwitter::status)
    val profiles = Lookup("profile", FxTwitter::profile)

    class Lookup<T : Any>(private val kind: String, private val fetch: (String) -> T?) {
        private class Entry<T>(val value: T?, private val fetchedAt: Long) {
            val isFresh: Boolean
                get() = SystemClock.elapsedRealtime() - fetchedAt < if (value != null) FOUND_TTL_MS else MISSING_TTL_MS
        }

        private val entries =
            object : LinkedHashMap<String, Entry<T>>(64, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry<T>>) = size > MAX_ENTRIES
            }
        private val pending = HashMap<String, Future<T?>>()

        fun get(ids: Set<String>): Map<String, T> {
            val deadline = SystemClock.elapsedRealtime() + WAIT_MS
            val (cached, loads) =
                synchronized(this) {
                    val (fresh, stale) = ids.partition { entries[it]?.isFresh == true }
                    val cached = fresh.mapNotNull { id -> entries.getValue(id).value?.let { id to it } }
                    cached to stale.associateWith(::pendingLoad)
                }
            val loaded = loads.mapNotNull { (id, load) -> load.await(deadline)?.let { id to it } }
            return (cached + loaded).toMap()
        }

        private fun pendingLoad(id: String): Future<T?> = pending.getOrPut(id) { network.submit(Callable { load(id) }) }

        private fun load(id: String): T? {
            val value =
                runCatching { fetch(id) }
                    .onFailure { Logger.warn(it) { "FxTwitter $kind lookup failed for $id" } }
                    .getOrNull()
            synchronized(this) {
                entries[id] = Entry(value, SystemClock.elapsedRealtime())
                pending.remove(id)
            }
            return value
        }

        private fun Future<T?>.await(deadline: Long): T? =
            try {
                get((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                null
            }
    }
}
