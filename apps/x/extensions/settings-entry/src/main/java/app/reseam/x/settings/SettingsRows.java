// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.x.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** List helpers for patch code that builds X settings sections; the model classes are the app's. */
public final class SettingsRows {
    private SettingsRows() {}

    public static List<Object> single(Object item) {
        return Collections.singletonList(item);
    }

    public static List<Object> withSection(List<?> sections, Object section) {
        List<Object> result = new ArrayList<>(sections);
        result.add(section);
        return result;
    }
}
