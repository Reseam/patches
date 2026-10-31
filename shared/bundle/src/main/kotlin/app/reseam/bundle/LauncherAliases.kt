// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Makes [selected] the app's launcher entry: enables it, then disables the other aliases whose class
 * names start with [prefix]. Enabling first means switching always leaves a launcher entry.
 */
fun Context.selectLauncherAlias(prefix: String, selected: String) {
    val manager = packageManager
    manager.setComponentState(ComponentName(packageName, selected), PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    manager
        .getPackageInfo(packageName, PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS)
        .activities
        .orEmpty()
        .filter { it.name.startsWith(prefix) && it.name != selected }
        .forEach { manager.setComponentState(ComponentName(packageName, it.name), PackageManager.COMPONENT_ENABLED_STATE_DISABLED) }
}

private fun PackageManager.setComponentState(component: ComponentName, state: Int) {
    if (getComponentEnabledSetting(component) != state) setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
}
