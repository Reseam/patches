// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("GalleryDownloads")

package app.reseam.x.download

import android.app.Activity
import android.app.Application
import android.os.Bundle
import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import java.lang.ref.WeakReference

/** X's own save for one gallery item, so each item keeps its photo, GIF or video handling. */
@Export external fun saveItem(gallery: Any, item: Any)

private var resumed = WeakReference<Activity>(null)
private var saving = false

// The gallery only holds the application context, which cannot show a dialog.
@Export
fun init(app: Application) {
    app.registerActivityLifecycleCallbacks(
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                resumed = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (resumed.get() === activity) resumed.clear()
            }

            override fun onActivityCreated(activity: Activity, state: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        }
    )
}

/** Returns true when it took over the save. */
@Export
fun offerSaveAll(gallery: Any, item: Any, media: List<*>?): Boolean {
    if (saving || !XSettings.saveAllMedia.value) return false
    val items = media?.filterNotNull()?.takeIf { it.size > 1 } ?: return false
    val activity = resumed.get() ?: return false
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
