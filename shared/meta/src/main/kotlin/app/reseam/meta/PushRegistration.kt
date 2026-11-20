// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PushRegistration")

package app.reseam.meta

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.reseam.runtime.Export

// Android 12 refuses a PendingIntent without a mutability flag. Play services only reads the sender from this one.
@Export
fun immutableBroadcast(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent =
    PendingIntent.getBroadcast(context, requestCode, intent, flags or PendingIntent.FLAG_IMMUTABLE)
