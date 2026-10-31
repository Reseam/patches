// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("LayoutFeedSubtitle")

package app.reseam.youtube.hidelayout

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.FeedSettings
import app.reseam.youtube.settings.PlayerSettings

@Export
fun modifyFeedSubtitleSpan(original: CharSequence?, dimension: Float): CharSequence? {
    val hideViews = FeedSettings.hideViewCount.value
    val hideUpload = PlayerSettings.hideUploadTime.value
    if (original == null || (!hideViews && !hideUpload)) return original
    if (dimension != 16f && dimension != 42f) return original
    val parts = subtitleParts(original) ?: return original
    val builder = SpannableStringBuilder(original)
    if (hideUpload) builder.delete(parts.uploadStart, builder.length)
    if (hideViews) builder.delete(parts.viewStart, parts.uploadStart)
    Logger.debug { "LayoutComponentsFilter modified subtitle: $original" }
    return SpannableString(builder)
}

// Newer subtitles use space runs with badge and view-count icon spans, instead of middle dots.
private data class SubtitleParts(val viewStart: Int, val uploadStart: Int)

private fun subtitleParts(subtitle: CharSequence): SubtitleParts? {
    val delimiter = " · "
    val firstDelimiter = TextUtils.indexOf(subtitle, delimiter)
    if (firstDelimiter >= 0) {
        val uploadStart = TextUtils.indexOf(subtitle, delimiter, firstDelimiter + delimiter.length)
        if (
            uploadStart < 0 ||
                TextUtils.indexOf(subtitle, delimiter, uploadStart + delimiter.length) >= 0
        )
            return null
        return SubtitleParts(firstDelimiter, uploadStart)
    }
    val runs = Regex(" {2,}").findAll(subtitle).map { it.range }.toList()
    if (runs.size != 2) return null
    val viewsText = runs[0].last + 1
    val viewStart =
        if (subtitle is Spanned) {
            subtitle
                .getSpans(runs[0].first, viewsText, Any::class.java)
                .minOfOrNull { span ->
                    val start = subtitle.getSpanStart(span)
                    if (subtitle.getSpanEnd(span) == viewsText && start > runs[0].first) start
                    else viewsText
                }
                ?.coerceAtMost(viewsText) ?: viewsText
        } else viewsText
    return SubtitleParts(viewStart, runs[1].first)
}
