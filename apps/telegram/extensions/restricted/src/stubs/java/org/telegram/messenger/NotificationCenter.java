// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.telegram.messenger;

public class NotificationCenter {
    public static int fileLoaded;
    public static int fileLoadFailed;

    public static NotificationCenter getInstance(int num) { return null; }
    public void addObserver(NotificationCenterDelegate observer, int id) {}
    public void removeObserver(NotificationCenterDelegate observer, int id) {}

    public interface NotificationCenterDelegate {
        void didReceivedNotification(int id, int account, Object... args);
    }
}
