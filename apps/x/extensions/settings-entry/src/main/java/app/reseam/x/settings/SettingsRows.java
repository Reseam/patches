// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** List helpers for patch code that builds X settings sections; the model classes are the app's. */
public final class SettingsRows {
    private SettingsRows() {}

    // Built from X's own row models, so the body is emitted by the settings entry patch.
    public static Object section() {
        throw new UnsupportedOperationException("implemented by the settings entry patch");
    }

    public static List<Object> single(Object item) {
        return Collections.singletonList(item);
    }

    // A data-class copy passes the list back in, and it already has the section.
    @SuppressWarnings("unchecked")
    public static List<Object> withSection(List<?> sections, Object section) {
        if (sections instanceof WithReseamSection) return (List<Object>) sections;
        WithReseamSection result = new WithReseamSection(sections);
        result.add(section);
        return result;
    }

    private static final class WithReseamSection extends ArrayList<Object> {
        WithReseamSection(List<?> sections) {
            super(sections);
        }
    }
}
