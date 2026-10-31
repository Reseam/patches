// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import android.content.res.Resources
import app.reseam.bundle.dpToPx

fun dp(value: Float): Int = Resources.getSystem().dpToPx(value, rounded = false)
