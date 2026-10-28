// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("NavigationButtons")

package app.reseam.youtube.navbuttons

import android.os.Build
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import app.reseam.bundle.Logger
import app.reseam.bundle.isNightMode
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.debug
import app.reseam.youtube.navigation.NavigationBar.NavigationButton
import app.reseam.youtube.settings.AppearanceSettings

@Export
fun navigationTabCreated(button: NavigationButton?, view: View?) {
    val hidden = hiddenTabs[button]?.invoke() == true
    Logger.debug { "Navigation button created: $button hidden=$hidden" }
    if (hidden) view?.visibility = View.GONE
}

@Export
fun hideNavigationButtonLabels(view: TextView?) {
    val hidden = AppearanceSettings.hideNavigationButtonLabels.value
    Logger.debug { "Navigation button labels hidden=$hidden" }
    if (hidden) view?.visibility = View.GONE
}

@Export
fun useAnimatedNavigationButtons(original: Boolean): Boolean =
    AppearanceSettings.navigationBarAnimations.value.also {
        Logger.debug { "Navigation button animations=$it" }
    }

@Export
fun enableNarrowNavigationButton(original: Boolean): Boolean {
    val narrow = AppearanceSettings.narrowNavigationButtons.value
    val result = narrow || original
    Logger.debug { "Narrow navigation buttons: $original -> $result" }
    return result
}

@Export
fun useTranslucentNavigationStatusBar(original: Boolean): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return original
    val disabled = AppearanceSettings.disableTranslucentStatusBar.value
    val result = !disabled && original
    Logger.debug { "Translucent status bar: $original -> $result" }
    return result
}

@Export
fun useTranslucentNavigationButtons(original: Boolean): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return original
    val darkDisabled = AppearanceSettings.disableTranslucentNavigationBarDark.value
    val lightDisabled = AppearanceSettings.disableTranslucentNavigationBarLight.value
    if (darkDisabled == lightDisabled) return original && !darkDisabled
    val dark = YouTubeContext.applicationContext.resources.isNightMode()
    val disabled = if (dark) darkDisabled else lightDisabled
    val result = original && !disabled
    Logger.debug { "Translucent navigation buttons: $original -> $result" }
    return result
}

@Export
fun hideCreateButton(button: Enum<*>?, menuItem: MenuItem?) = ToolbarButton.CREATE.hide(button, menuItem)

@Export
fun hideNotificationButton(button: Enum<*>?, menuItem: MenuItem?) =
    ToolbarButton.NOTIFICATION.hide(button, menuItem)

@Export
fun hideSearchButton(button: Enum<*>?, menuItem: MenuItem?) = ToolbarButton.SEARCH.hide(button, menuItem)

@Export
fun hideMessagesButton(button: Enum<*>?, menuItem: MenuItem?) = ToolbarButton.MESSAGES.hide(button, menuItem)

private enum class ToolbarButton(
    val kind: String,
    val enabled: () -> Boolean,
    vararg val names: String,
) {
    CREATE(
        "create",
        { AppearanceSettings.hideToolbarCreateButton.value },
        "CREATION_ENTRY",
        "FAB_CAMERA",
    ),
    NOTIFICATION(
        "notification",
        { AppearanceSettings.hideToolbarNotificationButton.value },
        "TAB_ACTIVITY",
        "TAB_ACTIVITY_CAIRO",
    ),
    SEARCH(
        "search",
        { AppearanceSettings.hideToolbarSearchButton.value },
        "SEARCH",
        "SEARCH_CAIRO",
        "SEARCH_BOLD",
    ),
    MESSAGES(
        "messages",
        { AppearanceSettings.hideToolbarMessagesButton.value },
        "MESSAGE_BUBBLE_OVERLAP",
    );

    // Hiding the menu item drops its slot; the menu presenter makes the action view visible again.
    fun hide(button: Enum<*>?, menuItem: MenuItem?) {
        val enabled = enabled()
        val hidden = enabled && button?.name in names
        Logger.debug { "Navigation toolbar $kind: ${button?.name ?: "null"} hidden=$hidden" }
        if (hidden) menuItem?.setVisible(false)
    }
}

private val hiddenTabs =
    mapOf(
        NavigationButton.HOME to { AppearanceSettings.hideHomeButton.value },
        NavigationButton.SHORTS to { AppearanceSettings.hideShortsButton.value },
        NavigationButton.CREATE to { AppearanceSettings.hideCreateButton.value },
        NavigationButton.SUBSCRIPTIONS to { AppearanceSettings.hideSubscriptionsButton.value },
        NavigationButton.NOTIFICATIONS to { AppearanceSettings.hideNotificationsButton.value },
        NavigationButton.LIBRARY to { AppearanceSettings.hideLibraryButton.value },
    )
