// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.download

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

private var resumed = WeakReference<Activity>(null)
private var tracking = false

/** Dialogs need an activity: the application context cannot show one. */
internal fun resumedActivity(): Activity? = resumed.get()

internal fun trackResumedActivity(app: Application) {
    if (tracking) return
    tracking = true
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
