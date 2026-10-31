// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("InstagramSettingsEntry")

package app.reseam.instagram.settings

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.identifier
import app.reseam.runtime.Export

private const val RESEAM_VIEW_TAG = 0x7f5151cc
private const val RESEAM_DECOR_TAG = 0x7f5151cd
private const val RESEAM_REPOST_VISIBILITY_TAG = 0x7f5151ce
private val repostViewNames =
    arrayOf(
        "clips_repost_button",
        "repost_button",
        "repost_button_container",
        "row_feed_button_repost",
        "tisu_ufi_repost_button",
    )
private var hookInstalled = false
private var repostViewIds = intArrayOf()
private var profileActionBarId = 0
private var actionBarButtonsId = 0
private var profileButton: ProfileSettingsButton? = null

@Export
fun init(ctx: Context?) {
    val app = ctx?.applicationContext as? Application ?: return
    repostViewIds = repostViewNames.map { app.identifier(it, "id") }.toIntArray()
    profileActionBarId = app.identifier("profile_action_bar", "id")
    actionBarButtonsId = app.identifier("right_action_bar_buttons", "id")
    profileButton = ProfileSettingsButton(app)
    if (hookInstalled) return
    hookInstalled = true
    app.registerActivityLifecycleCallbacks(
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, state: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                attachOnceLaidOut(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        }
    )
}

private fun attachOnceLaidOut(activity: Activity) {
    val decor = activity.window?.decorView ?: return
    if (decor.getTag(RESEAM_DECOR_TAG) != null) return
    decor.setTag(RESEAM_DECOR_TAG, true)
    decor.viewTreeObserver.addOnGlobalLayoutListener {
        walk(decor, AppearanceSettings.hideRepostButtons.value)
    }
    walk(decor, AppearanceSettings.hideRepostButtons.value)
}

private fun walk(view: View?, hideRepost: Boolean) {
    if (view == null) return
    val id = view.id
    if (id != View.NO_ID && repostViewIds.any { it != 0 && it == id }) {
        view.updateRepostVisibility(hideRepost)
    }
    if (profileActionBarId != 0 && id == profileActionBarId) {
        // The profile menu button: the last action button, identified by layout since it has no id
        // and its description is localized.
        val buttons = view.findViewById<ViewGroup>(actionBarButtonsId)
        buttons?.getChildAt(buttons.childCount - 1)?.openSettingsOnLongClick()
    }
    profileButton?.attachTo(view)
    if (view is ViewGroup) {
        for (index in 0 until view.childCount) walk(view.getChildAt(index), hideRepost)
    }
}

private fun View.openSettingsOnLongClick() {
    if (getTag(RESEAM_VIEW_TAG) != null) return
    setTag(RESEAM_VIEW_TAG, true)
    isLongClickable = true
    setOnLongClickListener {
        ReseamPatches.bundle().openSettings()
        true
    }
}

private fun View.updateRepostVisibility(hide: Boolean) {
    val original = getTag(RESEAM_REPOST_VISIBILITY_TAG)
    when {
        hide -> {
            if (original == null) setTag(RESEAM_REPOST_VISIBILITY_TAG, visibility)
            if (visibility != View.GONE) visibility = View.GONE
        }
        original is Int -> {
            setTag(RESEAM_REPOST_VISIBILITY_TAG, null)
            visibility = original
        }
    }
}
