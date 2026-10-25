// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.points

/**
 * Keeps every element on litho. RenderNext is a second renderer whose views the litho filters never
 * see. The server sends an element to it by marking the element proto or the element's config, or
 * by listing its template for conversion. With both marks read as unset and the list read as empty,
 * every builder and presenter takes its litho branch, which renders the same element bytes.
 */
internal fun PatchRuntime.keepElementsOnLitho() {
    renderNextEnablementCheck.alwaysReturn(false)

    // Each caller of the proto check reads the config mark right before it, through the element.
    val configReads = renderNextEnablementCallers.points { calls(renderNextEnablementCheck) }.all
        .map { it.previous { opcode(Opcode.IGET_BOOLEAN) } }
    val configFlag = configReads.map { it.field().ref }.distinct().single()
    val element = configReads
        .map { it.previous { opcode(Opcode.IGET_OBJECT); field { type(configFlag.definingClass) } }.field().owner }
        .distinct()
        .single()

    // Every decision is made with the element in hand, so the methods that take it hold every read.
    methods("elementConsumers") { hasParam(element) }
        .points { field { owner(configFlag.definingClass); name(configFlag.name) } }.all
        .also { check(it.isNotEmpty()) { "No reads of the RenderNext config flag" } }
        .forEach { read ->
            read.captureAs("renderNextConfig", Type.Boolean).after { capture("renderNextConfig").assign(bool(false)) }
        }

    renderNextTemplateListReaders
        .points { literal(RENDER_NEXT_TEMPLATE_LIST_FLAG); then(within = 4) { resultOf(Type.String) } }.all
        .also { check(it.isNotEmpty()) { "No reads of the RenderNext template list" } }
        .forEach { read ->
            read.captureAs("renderNextTemplates", Type.String).after { capture("renderNextTemplates").assign(string("")) }
        }
}

// Comma separated templates to rebuild with RenderNext, each "template" or "parentTemplate:template".
private const val RENDER_NEXT_TEMPLATE_LIST_FLAG = 45661551L

private val renderNextEnablementCheck = method("renderNextEnablementCheck") {
    strings("Failed to read Element proto passed in to RenderNextEnablementCheck: ")
    returns(Type.Boolean)
}

private val renderNextEnablementCallers = methods("renderNextEnablementCallers") {
    calls(renderNextEnablementCheck)
}

private val renderNextTemplateListReaders = methods("renderNextTemplateListReaders") {
    literals(RENDER_NEXT_TEMPLATE_LIST_FLAG)
}
