// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("MediaDownloader")

package app.reseam.instagram.download

import android.content.Context
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.runtime.Export
import java.util.WeakHashMap

private val reelsMenus = WeakHashMap<Any, Boolean>()

@Export
fun isStoryDownload(option: CharSequence?): Boolean =
    option != null && InstagramStrings.downloadLabel.contentEquals(option)

@Export
fun appendStoryDownload(items: Array<out CharSequence?>?): Array<out CharSequence?> {
    val label = InstagramStrings.downloadLabel
    if (items == null) return arrayOf<CharSequence>(label)
    if (items.any(::isStoryDownload)) return items
    return (items.asList() + label).toTypedArray()
}

@Export
fun handleFeedMenuClick(media: Any?, option: Any?, context: Context?, currentIndex: Int): Boolean {
    if (!isDownload(option)) return false
    if (media != null) download(media, context, currentIndex)
    else showToast(context, InstagramStrings.downloadMediaUrlMissing)
    return true
}

@Export fun downloadMedia(media: Any?, context: Context?) = download(media, context)

@Export fun downloadStory(owner: Any?) = downloadStoryFromOwner(owner)

@Export
fun claimReelsMenu(menu: Any?): Boolean {
    if (menu == null) return false
    return synchronized(reelsMenus) { reelsMenus.put(menu, true) == null }
}

@Export
fun hasDownloadOption(options: List<*>?, option: Any?): Boolean {
    if (options == null || option == null) return false
    return options.any { it === option || it != null && rowOption(it) === option }
}

@Export
fun labelDownloadOption(options: List<*>?, downloadOption: Any?) {
    if (options == null || downloadOption == null) return
    options.forEach { row ->
        try {
            if (row != null && option(row) === downloadOption) {
                setLabel(row, InstagramStrings.downloadLabel)
            }
        } catch (_: ClassCastException) {}
    }
}

@Export
fun withDownloadOption(options: List<*>?, option: Any?): List<*>? {
    if (options == null || option == null || options.contains(option)) return options
    return options + option
}

private fun rowOption(row: Any): Any? =
    try {
        option(row)
    } catch (_: ClassCastException) {
        null
    }
