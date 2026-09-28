// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method

/**
 * The composable a feed section renders through, (feed context, composer, changed flags) -> void, in
 * the section class that loads [label] (its toString label or item key). Returning from it early
 * leaves the section in the list with nothing drawn.
 */
fun feedSectionContent(label: String) = method("$label content") {
    inClass(klass(label) { strings(label) })
    returns(Type.Void)
    paramCount(3)
    param(2, Type.Int)
}
