// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.api

import android.os.SystemClock
import app.reseam.bundle.Logger
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val WAIT_MS = 4_000L
private const val FOUND_TTL_MS = 30 * 60_000L
private const val MISSING_TTL_MS = 5 * 60_000L
private const val MAX_ENTRIES = 1_000

private val network =
    Executors.newFixedThreadPool(4) { runnable -> Thread(runnable, "Reseam-GraphQL").apply { isDaemon = true } }

/**
 * Values a rewriter fetches from another service, cached by key.
 *
 * X holds the screen until its response returns, so a lookup waits a fixed budget. A fetch that
 * finishes later still lands in the cache and shows on the next load.
 */
class RemoteLookup<K : Any, V : Any>(
    private val name: String,
    private val batches: (List<K>) -> List<List<K>>,
    private val fetch: (List<K>) -> Map<K, V>,
) {
    private class Entry<V>(val value: V?, private val fetchedAt: Long) {
        val isFresh: Boolean
            get() = SystemClock.elapsedRealtime() - fetchedAt < if (value != null) FOUND_TTL_MS else MISSING_TTL_MS
    }

    private val entries =
        object : LinkedHashMap<K, Entry<V>>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Entry<V>>) = size > MAX_ENTRIES
        }
    private val pending = HashMap<K, Future<Map<K, V>>>()

    fun get(keys: Set<K>): Map<K, V> {
        val deadline = SystemClock.elapsedRealtime() + WAIT_MS
        val (cached, loads) =
            synchronized(this) {
                val (fresh, stale) = keys.partition { entries[it]?.isFresh == true }
                val cached = fresh.mapNotNull { key -> entries.getValue(key).value?.let { key to it } }
                val (waiting, missing) = stale.partition { it in pending }
                val started = batches(missing).flatMap(::start)
                cached to (waiting.associateWith(pending::getValue) + started)
            }
        val loaded = loads.mapNotNull { (key, load) -> load.await(deadline)?.get(key)?.let { key to it } }
        return (cached + loaded).toMap()
    }

    /** Fetches one batch in the background, and returns the shared load for each of its keys. */
    private fun start(batch: List<K>): List<Pair<K, Future<Map<K, V>>>> {
        val load = network.submit(Callable { load(batch) })
        batch.forEach { pending[it] = load }
        return batch.map { it to load }
    }

    private fun load(batch: List<K>): Map<K, V> {
        val values =
            runCatching { fetch(batch) }
                .onFailure { Logger.warn(it) { "$name lookup failed for ${batch.size} keys" } }
                .getOrDefault(emptyMap())
        synchronized(this) {
            batch.forEach { key ->
                entries[key] = Entry(values[key], SystemClock.elapsedRealtime())
                pending.remove(key)
            }
        }
        return values
    }

    private fun Future<Map<K, V>>.await(deadline: Long): Map<K, V>? =
        try {
            get((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            null
        }
}
