// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Counts")

package app.reseam.tiktok.interaction

import app.reseam.runtime.Export
import java.text.NumberFormat

@Export fun exact(count: Long): String = NumberFormat.getIntegerInstance().format(count)
