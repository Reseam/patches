// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import android.content.Context
import android.net.ConnectivityManager

fun isNetworkConnected(): Boolean =
    (YouTubeContext.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager?)
        ?.activeNetwork != null
