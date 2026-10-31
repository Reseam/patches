// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.postheader

import app.reseam.runtime.Export

// X's Compose calls this through its own kotlin.jvm.functions.Function0, so this module stays off the
// bundle's relocated stdlib. The patch emits the body; nullable Unit keeps the JVM return type kotlin.Unit.
@Export
class DownloadClick(@JvmField val media: Any?) : () -> Unit? {
    @Export override external fun invoke(): Unit?
}
