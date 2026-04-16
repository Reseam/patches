// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media.download

import app.reseam.patch.FieldInfo
import app.reseam.patch.FieldRef
import app.reseam.patch.Method
import app.reseam.patch.PatchRuntime
import app.reseam.patch.buildInstructions

private const val MEDIA_META = "Lapp/reseam/instagram/download/MediaMeta;"
private const val OBJECT_TYPE = "Ljava/lang/Object;"

internal fun hookListenerBridges(ctx: PatchRuntime) {
    val listenerMethod = feedMenuClickListenerMethod(ctx)
    val listenerClass = listenerMethod.info.classDescriptor
    val listenerDef = ctx.bytecode.findClass(listenerClass)
        ?: error("Feed listener class not found: $listenerClass")

    // Shared-lambda listener classes hold their captured values in untyped
    // Object fields (e.g. LX/EAR.A00/A01). Collect them in declaration order.
    val slotFields = listenerDef.instanceFields
        .filter { it.fieldType == OBJECT_TYPE }
    if (slotFields.isEmpty()) {
        error("Feed listener $listenerClass has no Object slot fields; layout changed")
    }

    val handlerClass = feedClickHandlerFingerprint.method.info.classDescriptor
    val handlerMediaField = feedMediaField(feedClickHandlerFingerprint.method)

    rewriteListenerMedia(ctx, listenerClass, slotFields, handlerClass, handlerMediaField)
    rewriteListenerDownloadOption(ctx, listenerClass, slotFields)
}

// Tries each Object slot on the listener: if it's an instance of the feed
// click handler (LX/EiP;), read the handler's media field and return it.
// No tag-ordinal parsing — works regardless of which switch case the download
// click occupies, which is what actually varies across IG builds.
private fun rewriteListenerMedia(
    ctx: PatchRuntime,
    listenerClass: String,
    slotFields: List<FieldInfo>,
    handlerClass: String,
    handlerMediaField: FieldRef,
) {
    val bridge = findBridge(ctx, "listenerMedia")
    bridge.replaceBody(registersSize = 3, outsSize = 0, insns = buildInstructions {
        checkCast(2, listenerClass)
        for ((i, slot) in slotFields.withIndex()) {
            val nextLabel = "lm_next_$i"
            igetObject(0, 2, FieldRef(listenerClass, slot.name, slot.fieldType))
            instanceOf(1, 0, handlerClass)
            ifEqz(1, nextLabel)
            checkCast(0, handlerClass)
            igetObject(0, 0, handlerMediaField)
            returnObject(0)
            label(nextLabel)
        }
        const_(0, 0)
        returnObject(0)
    })
    ctx.log.info(
        "Bound MediaMeta.listenerMedia -> $listenerClass (${slotFields.size} slots) via $handlerClass.${handlerMediaField.name}"
    )
}

// Tries each Object slot: if it's a MediaOption$Option, return it.
private fun rewriteListenerDownloadOption(
    ctx: PatchRuntime,
    listenerClass: String,
    slotFields: List<FieldInfo>,
) {
    val bridge = findBridge(ctx, "listenerDownloadOption")
    bridge.replaceBody(registersSize = 3, outsSize = 0, insns = buildInstructions {
        checkCast(2, listenerClass)
        for ((i, slot) in slotFields.withIndex()) {
            val nextLabel = "ldo_next_$i"
            igetObject(0, 2, FieldRef(listenerClass, slot.name, slot.fieldType))
            instanceOf(1, 0, MEDIA_OPTION_TYPE)
            ifEqz(1, nextLabel)
            returnObject(0)
            label(nextLabel)
        }
        const_(0, 0)
        returnObject(0)
    })
    ctx.log.info(
        "Bound MediaMeta.listenerDownloadOption -> $listenerClass (${slotFields.size} slots)"
    )
}

private fun findBridge(ctx: PatchRuntime, name: String): Method =
    ctx.bytecode.findClass(MEDIA_META)
        ?.methods
        ?.firstOrNull { it.info.methodName == name }
        ?: error("MediaMeta.$name bridge not found")
