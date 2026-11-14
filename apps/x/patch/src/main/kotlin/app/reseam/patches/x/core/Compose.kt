// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.CodeScope
import app.reseam.patch.MethodLocal
import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.ValueRef
import app.reseam.patch.before
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.reserveLocal

const val COMPOSER = "Landroidx/compose/runtime/Composer;"
const val MODIFIER = "Landroidx/compose/ui/Modifier;"
const val FUNCTION0 = "Lkotlin/jvm/functions/Function0;"
const val FUNCTION1 = "Lkotlin/jvm/functions/Function1;"

val xIcon = klass("xIcon") { strings("Icon(drawableRes=") }

// Modifier.Companion, the empty modifier: the Modifier whose toString is "Modifier", held in its own static field.
private val modifierCompanionClass = klass("modifierCompanion") {
    strings("Modifier")
    implements(MODIFIER)
}
private val modifierCompanion = fieldTarget("modifierCompanion") {
    modifierCompanionClass.classDef.staticFields.single { it.fieldType == modifierCompanionClass.descriptor }.ref
}

private val androidViewComposable = method("androidView") {
    params(FUNCTION1, MODIFIER, FUNCTION1, COMPOSER, Type.Int, Type.Int)
    returns(Type.Void)
    custom { owner.startsWith("Landroidx/compose/ui/viewinterop/") }
}

/** Composes the Android view [factory] makes, handing it to [update] on every recomposition. */
fun CodeScope.androidView(factory: ValueRef, update: ValueRef, composer: ValueRef) {
    call(androidViewComposable, factory, staticField(modifierCompanion), update, composer, int(0), int(0))
}

// Big composables reuse their Composer parameter's register, so it is kept from entry.
fun MethodTarget.composerAtEntry(): MethodLocal =
    reserveLocal("composer", COMPOSER).also { local -> before { local(local).assign(paramOfType(COMPOSER)) } }
