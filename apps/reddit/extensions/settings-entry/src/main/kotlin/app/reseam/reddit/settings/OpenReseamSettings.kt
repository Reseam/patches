// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.settings

import app.reseam.runtime.Export

@Export
class OpenReseamSettings : () -> Unit? {
    // R8 renames the Unit singleton, so the patch resolves it and returns it.
    // Nullable Unit keeps the JVM return type kotlin.Unit instead of void.
    @Export override external fun invoke(): Unit?
}
