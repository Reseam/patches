// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.patch

private const val RESIZEABLE_ACTIVITY = "android:resizeableActivity"

val allowSplitScreen = patch("Allow split screen") {
    description("Lets the app open in split screen and in bubble windows instead of only full screen.")

    execute {
        var activities = 0

        for (name in manifest.components()) {
            val component = manifest.component(name)
            component.edit {
                if (component.splitName == null) {
                    val application = findByTag("application").firstOrNull() ?: error("manifest has no <application> element")
                    application.setBool(RESIZEABLE_ACTIVITY, true)
                }
                for (activity in findByTag("activity")) {
                    if (activity[RESIZEABLE_ACTIVITY] == null) continue
                    activity.setBool(RESIZEABLE_ACTIVITY, true)
                    activities++
                }
            }
        }

        log.info("App and $activities activities are now resizable")
    }
}
