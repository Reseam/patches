// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.MethodLocal
import app.reseam.patch.MethodTarget
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.reserveLocal

const val COMPOSER = "Landroidx/compose/runtime/Composer;"
const val MODIFIER = "Landroidx/compose/ui/Modifier;"

val xIcon = klass("xIcon") { strings("Icon(drawableRes=") }

// Big composables reuse their Composer parameter's register, so it is kept from entry.
fun MethodTarget.composerAtEntry(): MethodLocal =
    reserveLocal("composer", COMPOSER).also { local -> before { local(local).assign(paramOfType(COMPOSER)) } }
