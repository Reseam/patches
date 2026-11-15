// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.core

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

private const val FUNCTION1 = "Lkotlin/jvm/functions/Function1;"

// AndroidView keeps its package while Reddit renames Compose's own types, so Modifier and Composer are read
// off its parameters: (factory, modifier, update, composer, changed, default).
private val androidViewComposable = method("androidView") {
    returns(Type.Void)
    paramCount(6)
    param(0, FUNCTION1)
    param(2, FUNCTION1)
    param(4, Type.Int)
    param(5, Type.Int)
    custom { owner.startsWith("Landroidx/compose/ui/viewinterop/") }
}

val composer: String get() = androidViewComposable.parameterTypes[3]

private val modifier: String get() = androidViewComposable.parameterTypes[1]

// Modifier.Companion, the empty modifier: the Modifier whose toString is "Modifier", held in its own static field.
private val modifierCompanionClass = klass("modifierCompanion") {
    strings("Modifier")
    implements(modifier)
}
private val modifierCompanion = fieldTarget("modifierCompanion") {
    modifierCompanionClass.classDef.staticFields.single { it.fieldType == modifierCompanionClass.descriptor }.ref
}

/** Composes the Android view [factory] makes, handing it to [update] on every recomposition. */
fun CodeScope.androidView(factory: ValueRef, update: ValueRef, composer: ValueRef) {
    call(androidViewComposable, factory, staticField(modifierCompanion), update, composer, int(0), int(0))
}

/** The one class implementing Composer, which inlined layouts pass around. */
val composerImpl: String get() = klass("composerImpl") { implements(composer) }.descriptor

// Big composables reuse their Composer parameter's register, so it is kept from entry.
fun MethodTarget.composerAtEntry(): MethodLocal =
    reserveLocal("composer", composer).also { local -> before { local(local).assign(paramOfType(composer)) } }

/** For a composable lambda, whose Composer arrives untyped as parameter [index]. */
fun MethodTarget.composerAtEntry(index: Int): MethodLocal =
    reserveLocal("composer", composer).also { local -> before { local(local).assign(param(index).cast(composer)) } }
