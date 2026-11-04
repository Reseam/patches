// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("GalleryDownloads")

package app.reseam.x.download

import android.app.Application
import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

/** X's own save for one gallery item, so each item keeps its photo, GIF or video handling. */
@Export external fun saveItem(gallery: Any, item: Any)

private var saving = false

@Export
fun init(app: Application) {
    trackResumedActivity(app)
}

/** Returns true when it took over the save. */
@Export
fun offerSaveAll(gallery: Any, item: Any, media: List<*>?): Boolean {
    if (saving || !XSettings.saveAllMedia.value) return false
    val items = media?.filterNotNull()?.takeIf { it.size > 1 } ?: return false
    val activity = resumedActivity() ?: return false
    ReseamPatches.bundle().ui(activity).dialog()
        .title(XStrings.saveMediaTitle(items.size))
        .action(XStrings.saveMediaCurrent) { save(gallery, listOf(item)) }
        .action(XStrings.saveMediaAll) { save(gallery, items) }
        .show()
    return true
}

private fun save(gallery: Any, items: List<Any>) {
    saving = true
    try {
        items.forEach { saveItem(gallery, it) }
    } finally {
        saving = false
    }
}
