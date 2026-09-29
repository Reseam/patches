// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtMethod
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.patches.youtube.core.YOUTUBE

private val lithoTextHooks = mutableListOf<ExtMethod>()

/**
 * Rewrites the text a Litho text component shows. The hook is static
 * (StringBuilder path, CharSequence text) -> CharSequence; hooks run in registration order.
 */
fun hookLithoText(hook: ExtMethod) {
    require(hook.isStatic && hook.proto == "(Ljava/lang/StringBuilder;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;") {
        "A Litho text hook must be static (StringBuilder, CharSequence) -> CharSequence: $hook"
    }
    lithoTextHooks += hook
}

val lithoTextHook = patch {
    compatibleWith(YOUTUBE)

    afterDependents {
        if (lithoTextHooks.isEmpty()) return@afterDependents
        val hooks = lithoTextHooks.toList()
        lithoTextHooks.clear()

        val contextTypes = setOf(conversionContext.descriptor, conversionContext.classDef.info.superclass)
        val contextField = fieldTarget("text conversion context") {
            textComponent.classDef.instanceFields.single { it.fieldType in contextTypes }.ref
        }
        // Read only the resolved path, not the expensive context.toString(). Save it at entry:
        // current releases reuse the incoming this register.
        val path = textLookup.reserveLocal("lithoTextPath", "java.lang.StringBuilder")
        textLookup.before {
            local(path).assign(nullObject)
            val source = thisObject.field(contextField)
            whenNotNull(source) {
                local(path).assign(source.cast(conversionContext.descriptor).field(conversionContextPath))
            }
        }
        textLookup.point("cached text span") {
            opcode(Opcode.IPUT_OBJECT)
            field { type(Type.CharSequence) }
        }.captureAs("text", Type.CharSequence).before {
            hooks.forEach { capture("text").assign(call(it, local(path), capture("text"))) }
        }

        // Both TextComponent and TextViewComponent call this formatter. Following the call
        // covers the new rendering path without depending on the experiment flag or register offsets.
        val textFormatter = textLookup.point("text formatter") {
            invokeStatic { returns(Type.CharSequence); hasParam(contextField.ref.fieldType) }
        }.callee()
        check(textFormatter.parameterTypes.first() == contextField.ref.fieldType)
        textFormatter.after {
            whenNotNull(param(0)) {
                val formatterPath = param(0).cast(conversionContext.descriptor).field(conversionContextPath)
                hooks.forEach { capture("result").assign(call(it, formatterPath, capture("result"))) }
                returnValue(capture("result"))
            }
        }
    }
}

private val textComponent = klass("text component") { strings("TextComponent") }
private val textLookup = method("cached text component") {
    inClass(textComponent)
    strings("…")
    flags(AccessFlags.PROTECTED or AccessFlags.FINAL)
    paramCount(1)
}
