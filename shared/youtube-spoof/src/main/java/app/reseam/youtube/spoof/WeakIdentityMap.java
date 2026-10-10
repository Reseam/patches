// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/** Associates native owners by identity, without retaining them or invoking protobuf equality. */
final class WeakIdentityMap<V> {
    private final ReferenceQueue<Object> collected = new ReferenceQueue<>();
    private final Map<Key, V> values = new HashMap<>();

    synchronized V get(Object owner) {
        prune();
        return owner == null ? null : values.get(new Key(owner, null));
    }

    synchronized void put(Object owner, V value) {
        prune();
        if (owner != null) values.put(new Key(owner, collected), value);
    }

    synchronized void remove(Object owner) {
        prune();
        if (owner != null) values.remove(new Key(owner, null));
    }

    private void prune() {
        Object reference;
        while ((reference = collected.poll()) != null) values.remove(reference);
    }

    private static final class Key extends WeakReference<Object> {
        private final int hash;

        Key(Object owner, ReferenceQueue<Object> queue) {
            super(owner, queue);
            hash = System.identityHashCode(owner);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            Object owner = get();
            return owner != null && other instanceof Key key && owner == key.get();
        }
    }
}
