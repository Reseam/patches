// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicInteger

private const val PROGRESS_MAX = 1000
// The system drops an app's notifications above about five a second, which would lose the final one.
private const val PROGRESS_INTERVAL_MS = 1000L
// Shares the app's notification id space; a high base keeps clear of its own ids.
private val nextId = AtomicInteger(0x5A7E0000)

/** Notifications for long work: progress while it runs, then the result in its place. */
class ProgressNotifications(private val context: Context, private val channel: String, channelName: String) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val progressPosted = HashMap<Int, Long>()

    init {
        manager.createNotificationChannel(NotificationChannel(channel, channelName, NotificationManager.IMPORTANCE_LOW))
    }

    fun newId(): Int = nextId.incrementAndGet()

    fun progress(id: Int, title: String, text: String, fraction: Float) {
        val now = SystemClock.elapsedRealtime()
        val last = progressPosted[id]
        if (last != null && now - last < PROGRESS_INTERVAL_MS) return
        progressPosted[id] = now
        post(
            id,
            builder(android.R.drawable.stat_sys_download, title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(PROGRESS_MAX, (fraction * PROGRESS_MAX).toInt(), false),
        )
    }

    fun finished(id: Int, title: String, open: Intent, openLabel: String) {
        val intent =
            PendingIntent.getActivity(
                context,
                id,
                open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
        post(
            id,
            builder(android.R.drawable.stat_sys_download_done, title)
                .setContentIntent(intent)
                .addAction(Notification.Action.Builder(null, openLabel, intent).build())
                .setAutoCancel(true),
        )
    }

    fun failed(id: Int, title: String, reason: String?) =
        post(id, builder(android.R.drawable.stat_notify_error, title).setContentText(reason).setAutoCancel(true))

    fun dismiss(id: Int) {
        progressPosted.remove(id)
        manager.cancel(id)
    }

    private fun builder(icon: Int, title: String): Notification.Builder =
        Notification.Builder(context, channel).setSmallIcon(icon).setContentTitle(title)

    private fun post(id: Int, builder: Notification.Builder) = manager.notify(id, builder.build())
}
