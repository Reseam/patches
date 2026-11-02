// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.comments

import app.reseam.runtime.Export

// The comment menu runs its items through Instagram's own kotlin.jvm.functions.Function0, so this
// module stays off the bundle's relocated stdlib. The patch emits the body.
@Export
class CopyCommentClick(@JvmField val text: String?) : () -> Any? {
    @Export override external fun invoke(): Any?
}
