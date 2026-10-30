// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.settings

import app.reseam.runtime.Export

// The Export-login row's click handler; the patch emits its body. Nullable Unit keeps the JVM return
// type kotlin.Unit instead of void, as X's Function0 expects.
@Export
class ExportLogin : () -> Unit? {
    @Export override external fun invoke(): Unit?
}
