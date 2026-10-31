// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.takeUnless { it === this }?.findActivity()
        else -> null
    }
