// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import android.graphics.Color
import app.reseam.runtime.TextValue

/** The setting as a color; an unparsable value falls back to the declared default. */
val TextValue.color: Int
    get() = runCatching { Color.parseColor(value) }.getOrElse { Color.parseColor(default) }
