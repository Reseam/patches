// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
