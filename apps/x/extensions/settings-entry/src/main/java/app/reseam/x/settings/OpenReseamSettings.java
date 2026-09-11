// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.x.settings;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/**
 * The click handler of the Reseam row in X's settings list. The body is emitted by the patch:
 * R8 renames the Unit singleton, so the patch resolves it and returns it.
 */
public final class OpenReseamSettings implements Function0<Unit> {
    @Override
    public Unit invoke() {
        throw new UnsupportedOperationException("implemented by the settings entry patch");
    }
}
