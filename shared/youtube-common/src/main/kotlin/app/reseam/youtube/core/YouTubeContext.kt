// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import android.app.Activity
import android.content.Context
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import java.lang.ref.WeakReference

object YouTubeContext {
    @Volatile private var context: Context? = null
    @Volatile private var activity = WeakReference<Activity>(null)

    @Export
    @JvmStatic
    fun init(applicationContext: Context) {
        context = applicationContext
        Logger.info { "Reseam extensions loaded" }
    }

    val applicationContext: Context
        get() = context ?: error("YouTubeContext.init has not run")

    @Export
    @JvmStatic
    fun setMainActivity(mainActivity: Activity) {
        activity = WeakReference(mainActivity)
    }

    /** The app's main window, where its pages, players and menus show; null before it is created. */
    val mainActivity: Activity?
        get() = activity.get()
}
