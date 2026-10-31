// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.settings

import app.reseam.runtime.Export
import com.reddit.settings.usersettings.UserSettingsSession

// Reddit's settings item interface is R8-named, so the patch adds it and its methods to this class.
@Export
class ReseamSettingsItem {
    companion object {
        @Export
        @JvmStatic
        fun sessions(): MutableSet<UserSettingsSession> =
            hashSetOf(
                UserSettingsSession.LoggedIn,
                UserSettingsSession.LoggedOut,
                UserSettingsSession.Incognito,
            )

        @Export
        @JvmStatic
        fun withItem(items: Set<Any>): MutableSet<Any> =
            LinkedHashSet(items).apply { add(ReseamSettingsItem()) }
    }
}
