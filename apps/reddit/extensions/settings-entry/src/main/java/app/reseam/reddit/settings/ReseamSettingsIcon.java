// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.reddit.settings;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

/**
 * The leading icon slot of the Reseam row. The patch fills {@link #draw} with a call to Reddit's
 * own icon composable and {@link #unit} with Reddit's Unit singleton, both of which R8 renames.
 */
public final class ReseamSettingsIcon implements Function2<Object, Object, Unit> {
    @Override
    public Unit invoke(Object composer, Object changed) {
        draw(composer);
        return unit();
    }

    static void draw(Object composer) {}

    static Unit unit() {
        throw new UnsupportedOperationException("implemented by the settings entry patch");
    }
}
