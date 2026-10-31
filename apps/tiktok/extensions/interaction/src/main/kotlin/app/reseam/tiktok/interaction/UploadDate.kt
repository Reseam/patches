// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("UploadDate")

package app.reseam.tiktok.interaction

import app.reseam.runtime.Export
import com.ss.android.ugc.aweme.feed.model.Aweme
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/** TikTok's own time text, when it shows one, followed by the post's date and region. */
@Export
fun postTime(text: String?, aweme: Aweme?): String? {
    val created = aweme?.createTime?.takeIf { it > 0 } ?: return text
    val date =
        DateFormat.getDateInstance(DateFormat.MEDIUM)
            .format(Date(TimeUnit.SECONDS.toMillis(created)))
    return listOfNotNull(
            text?.takeIf { it.isNotBlank() },
            date,
            aweme.region?.takeIf { it.isNotBlank() },
        )
        .joinToString(" · ")
}
