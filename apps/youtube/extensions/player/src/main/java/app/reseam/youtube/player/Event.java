// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.player;

import java.util.concurrent.CopyOnWriteArraySet;

/** Observers of one piece of player state. Observers may remove themselves while being notified. */
public final class Event<T> {
    public interface Observer<T> {
        void onEvent(T value);
    }

    private final CopyOnWriteArraySet<Observer<T>> observers = new CopyOnWriteArraySet<>();

    public void add(Observer<T> observer) {
        observers.add(observer);
    }

    public void remove(Observer<T> observer) {
        observers.remove(observer);
    }

    public void fire(T value) {
        for (Observer<T> observer : observers) observer.onEvent(value);
    }
}
