// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.reddit.settings;

import com.reddit.settings.usersettings.UserSettingsSession;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Reddit's settings item interface is R8-named, so the patch adds it and its methods to this class. */
public final class ReseamSettingsItem {
    public static Set<UserSettingsSession> sessions() {
        return new HashSet<>(Arrays.asList(UserSettingsSession.LoggedIn, UserSettingsSession.LoggedOut, UserSettingsSession.Incognito));
    }

    public static Set<Object> withItem(Set<?> items) {
        Set<Object> result = new LinkedHashSet<>(items);
        result.add(new ReseamSettingsItem());
        return result;
    }
}
