// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: CharSequence, duration: Int = Toast.LENGTH_LONG) {
    postToMainThread { Toast.makeText(this, message, duration).show() }
}
