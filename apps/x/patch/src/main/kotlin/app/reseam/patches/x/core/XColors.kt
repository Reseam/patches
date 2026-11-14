// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.invokeRegisters
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.regA
import app.reseam.patch.dex.regB
import app.reseam.patch.dex.stringValue
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.types.FieldRef

/** X's design system colors for the current theme, read from the composition like X's own screens read them. */
internal object XColors {
    val colors = klass("xColors") { strings("XColors(backgroundPrimary=") }

    val current = method("currentXColors") {
        params(COMPOSER)
        returns(colors.descriptor)
    }

    private val toString = method("xColorsToString") {
        inClass(colors)
        name("toString")
    }

    val backgroundSheets = labelled("backgroundSheets")
    val backgroundSecondary = labelled("backgroundSecondary")
    val foregroundPrimary = labelled("foregroundPrimary")
    val foregroundSecondary = labelled("foregroundSecondary")
    val foregroundInverted = labelled("foregroundInverted")
    val foregroundBrand = labelled("foregroundBrand")
    val borderNormal = labelled("borderNormal")
    val buttonPrimary = labelled("buttonPrimary")
    val isLight = labelled("isLight")

    private fun labelled(name: String) = fieldTarget("xColors.$name") {
        fieldsByLabel()[name] ?: error("XColors has no $name")
    }

    /**
     * R8 builds the toString by reading fields into registers, turning colors into strings, and passing labels
     * and values to outlined append helpers, each value after its label. Following registers from each field
     * read and each label to those calls pairs them, whatever order the reads happen in.
     */
    private fun fieldsByLabel(): Map<String, FieldRef> {
        val tokens = HashMap<Int, Any>()
        val labels = ArrayDeque<String>()
        val fields = HashMap<String, FieldRef>()
        var result: Any? = null
        for (instruction in toString.method.instructions) {
            val opcode = instruction.opcode ?: continue
            val target = instruction.regA
            val field = instruction.fieldRef?.takeIf { it.definingClass == colors.descriptor }
            // A wide value fills the register after its own too.
            if (target != null && "WIDE" in opcode.name) tokens.remove(target + 1)
            when {
                instruction.stringValue != null -> tokens[target!!] = instruction.stringValue!!
                field != null -> tokens[target!!] = field
                instruction.invokeRegisters != null -> {
                    val arguments = instruction.invokeRegisters!!.mapNotNull(tokens::get)
                    result = arguments.singleOrNull() as? FieldRef
                    for (token in arguments) {
                        when (token) {
                            is String -> if (token.endsWith("=")) labels.addLast(token)
                            is FieldRef -> labels.removeFirstOrNull()?.let { fields[it.substringAfter('(').removePrefix(", ").removeSuffix("=")] = token }
                        }
                    }
                }
                opcode.name.startsWith("MOVE_RESULT") && target != null -> result?.let { tokens[target] = it } ?: tokens.remove(target)
                opcode.name.startsWith("MOVE") && target != null -> tokens[instruction.regB]?.let { tokens[target] = it } ?: tokens.remove(target)
                target != null && !opcode.writesNothing() -> tokens.remove(target)
            }
        }
        return fields
    }

    private fun Opcode.writesNothing() =
        name.startsWith("IPUT") || name.startsWith("SPUT") || name.startsWith("APUT") || name.startsWith("IF_") ||
            name.startsWith("RETURN") || name.startsWith("GOTO") || this == Opcode.THROW
}
