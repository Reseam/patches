// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.opcode
import app.reseam.patch.methodTarget
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.before

/**
 * Forces the boolean property printed as [label] in a data class's toString (e.g. ", isDownloadable=")
 * to [value] while [toggle] is on. toString keeps the property name through obfuscation, and R8
 * inlines the getter, so the constructor stores are where every instance passes through.
 */
fun PatchRuntime.forceFlagWhen(label: String, toggle: ToggleSetting, value: Boolean) {
    val toStrings = methods("${label.trim(',', ' ', '=')}.toString") {
        name("toString")
        strings(label)
    }
    val hooked = toStrings.all.sumOf { toString ->
        val flag = toString.point { string(label) }.next { where { fieldRef != null } }.field()
        require(flag.type == Type.Boolean) { "$label is a ${flag.type} in ${toString.owner}, not a primitive boolean" }
        val owner = bytecode.findClass(toString.owner) ?: error("${toString.owner} missing")
        val constructors = owner.directMethods.filter { constructor ->
            constructor.name == "<init>" && constructor.instructions.any { it.opcode == Opcode.IPUT_BOOLEAN && it.fieldRef == flag.ref }
        }
        for (constructor in constructors) {
            methodTarget("${flag.name}:${owner.descriptor}") { constructor }
                .point { opcode(Opcode.IPUT_BOOLEAN); field { owner(flag.owner); name(flag.name) } }
                .captureAs("flag", Type.Boolean)
                .before(toggle) { capture("flag").assign(bool(value)) }
        }
        constructors.size
    }
    require(hooked > 0) { "no constructor stores $label" }
}
