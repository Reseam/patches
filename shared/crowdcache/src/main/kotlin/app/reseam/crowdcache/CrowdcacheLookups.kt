// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.crowdcache

import app.reseam.bundle.Logger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/**
 * Lookups in one namespace, gathered for a moment so items shown together cost one request. Each answer,
 * or null for a miss or a failed request, arrives on a background thread.
 */
class CrowdcacheLookups(private val namespace: String) {
    private val pending = LinkedHashMap<String, MutableList<(JSONObject?) -> Unit>>()
    private val thread = Executors.newSingleThreadScheduledExecutor()

    fun lookup(key: String, onResult: (JSONObject?) -> Unit) {
        val first = synchronized(pending) { pending.isEmpty().also { pending.getOrPut(key, ::ArrayList) += onResult } }
        if (first) thread.schedule(::send, GATHER_MS, TimeUnit.MILLISECONDS)
    }

    private fun send() {
        val batch = synchronized(pending) {
            pending.entries.take(Crowdcache.MAX_LOOKUP).associate { it.key to it.value }
                .also { pending.keys.removeAll(it.keys) }
                .also { if (pending.isNotEmpty()) thread.execute(::send) }
        }
        val found = runCatching { Crowdcache.lookup(namespace, batch.keys) }
            .onFailure { Logger.warn(it) { "Crowdcache $namespace lookup failed" } }
            .getOrDefault(emptyMap())
        batch.forEach { (key, callbacks) -> callbacks.forEach { it(found[key]) } }
    }

    private companion object {
        const val GATHER_MS = 150L
    }
}
