// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

internal class WeakIdentityMap<V> {
    private val collected = ReferenceQueue<Any>()
    private val values = HashMap<Key, V>()

    @Synchronized
    operator fun get(owner: Any?): V? {
        prune()
        return owner?.let { values[Key(it, null)] }
    }

    @Synchronized
    fun put(owner: Any?, value: V) {
        prune()
        if (owner != null) values[Key(owner, collected)] = value
    }

    @Synchronized
    fun remove(owner: Any?) {
        prune()
        if (owner != null) values.remove(Key(owner, null))
    }

    private fun prune() {
        generateSequence { collected.poll() as Key? }.forEach(values::remove)
    }

    private class Key(owner: Any, queue: ReferenceQueue<Any>?) : WeakReference<Any>(owner, queue) {
        private val hash = System.identityHashCode(owner)

        override fun hashCode(): Int = hash

        override fun equals(other: Any?): Boolean =
            this === other || get()?.let { owner -> other is Key && owner === other.get() } == true
    }
}
