// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.refs

import app.reseam.patch.DexClass
import app.reseam.patch.FieldRef
import app.reseam.patch.Instruction
import app.reseam.patch.Method
import app.reseam.patch.MethodRef
import app.reseam.patch.Opcodes
import app.reseam.patch.PatchRuntime
import app.reseam.patch.buildInstructions
import app.reseam.patch.compatibleWith
import app.reseam.patch.findInstructionsByInvoke
import app.reseam.patch.indexOfFirstInstruction
import app.reseam.patch.opcode
import app.reseam.patch.patch
import app.reseam.patch.typeRef
import app.reseam.patches.instagram.core.signatureCheckPatch

private const val REFS_MEDIA = "Lapp/reseam/instagram/refs/Media;"
private const val LIST_TYPE = "Ljava/util/List;"
private const val EXTENDED_IMAGE_URL_TYPE = "Lcom/instagram/model/mediasize/ExtendedImageUrl;"
private const val VIDEO_VERSION_INTF_TYPE = "Lcom/instagram/model/mediasize/VideoVersionIntf;"

val mediaRefs = patch(
    name = "Media refs",
    description = "Internal: binds app.reseam.instagram.refs.Media bridges to Instagram's media value class.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    dependsOn = listOf(signatureCheckPatch),
    enabledByDefault = false,
) {
    extendWith("instagram-refs.dex")

    execute { ctx ->
        val mediaClass = resolveMediaClass(ctx)
        val mediaClassDef = ctx.bytecode.findClass(mediaClass)
            ?: error("Media refs: class not found: $mediaClass")

        bindPhotoUrl(ctx, mediaClass, mediaClassDef)

        val dictField = resolveDictField(ctx, mediaClassDef)
        val videoGetter = resolveDictListGetter(ctx, dictField.fieldType, VIDEO_VERSION_INTF_TYPE)
            ?: error("Media refs: video_versions getter not correlated on ${dictField.fieldType}")
        val carouselGetter = resolveDictListGetter(ctx, dictField.fieldType, mediaClass)
            ?: error("Media refs: carousel_media getter not correlated on ${dictField.fieldType}")

        bindVideoUrl(ctx, mediaClass, dictField, videoGetter)
        bindChildren(ctx, mediaClass, dictField, carouselGetter)

        ctx.log.info(
            "Media refs bound: $mediaClass " +
                "(photo=${EXTENDED_IMAGE_URL_TYPE.substringAfterLast('/').trimEnd(';')}, " +
                "video=${dictField.fieldType}->${videoGetter.name}, " +
                "carousel=${dictField.fieldType}->${carouselGetter.name})"
        )
    }
}

private fun bindPhotoUrl(ctx: PatchRuntime, mediaClass: String, mediaClassDef: DexClass) {
    val imageField = mediaClassDef.instanceFields.firstOrNull { it.fieldType == EXTENDED_IMAGE_URL_TYPE }
        ?: error("Media refs: ExtendedImageUrl field not on $mediaClass")

    val bridge = bridgeMethod(ctx, "photoUrl")
    bridge.replaceBody(registersSize = 2, outsSize = 1, insns = buildInstructions {
        checkCast(1, mediaClass)
        igetObject(0, 1, FieldRef(mediaClass, imageField.name, imageField.fieldType))
        ifEqz(0, "ret_null")
        invokeVirtual(EXTENDED_IMAGE_URL_TYPE, "getUrl", "()Ljava/lang/String;", 0)
        moveResultObject(0)
        returnObject(0)
        label("ret_null")
        const_(0, 0)
        returnObject(0)
    })
}

private fun bindVideoUrl(
    ctx: PatchRuntime,
    mediaClass: String,
    dictField: FieldRef,
    videoGetter: MethodRef,
) {
    val bridge = bridgeMethod(ctx, "videoUrl")
    bridge.replaceBody(registersSize = 3, outsSize = 2, insns = buildInstructions {
        checkCast(2, mediaClass)
        igetObject(0, 2, dictField)
        ifEqz(0, "ret_null")
        invokeInterface(dictField.fieldType, videoGetter.name, videoGetter.proto, 0)
        moveResultObject(0)
        ifEqz(0, "ret_null")
        invokeInterface(LIST_TYPE, "isEmpty", "()Z", 0)
        moveResult(1)
        ifNez(1, "ret_null")
        const_(1, 0)
        invokeInterface(LIST_TYPE, "get", "(I)Ljava/lang/Object;", 0, 1)
        moveResultObject(0)
        ifEqz(0, "ret_null")
        checkCast(0, VIDEO_VERSION_INTF_TYPE)
        invokeInterface(VIDEO_VERSION_INTF_TYPE, "getUrl", "()Ljava/lang/String;", 0)
        moveResultObject(0)
        returnObject(0)
        label("ret_null")
        const_(0, 0)
        returnObject(0)
    })
}

private fun bindChildren(
    ctx: PatchRuntime,
    mediaClass: String,
    dictField: FieldRef,
    carouselGetter: MethodRef,
) {
    val bridge = bridgeMethod(ctx, "children")
    bridge.replaceBody(registersSize = 2, outsSize = 1, insns = buildInstructions {
        checkCast(1, mediaClass)
        igetObject(0, 1, dictField)
        ifEqz(0, "ret_null")
        invokeInterface(dictField.fieldType, carouselGetter.name, carouselGetter.proto, 0)
        moveResultObject(0)
        returnObject(0)
        label("ret_null")
        const_(0, 0)
        returnObject(0)
    })
}

private fun bridgeMethod(ctx: PatchRuntime, name: String): Method =
    ctx.bytecode.findClass(REFS_MEDIA)
        ?.methods
        ?.firstOrNull { it.info.methodName == name }
        ?: error("Media refs: stub $name not found")

// Media class is the field type of the first IGET_OBJECT in the share-URL
// carrier method (strings "https://www.instagram.com/p/" + "unknown"). This
// mirrors UserRefs: independent discovery, no shared fingerprint handle.
private fun resolveMediaClass(ctx: PatchRuntime): String {
    val carrier = shareUrlCarrierMethod(ctx)
    val idx = carrier.indexOfFirstInstruction {
        this is Instruction.RegField && value0.opcode.toInt() == Opcodes.IGET_OBJECT
    }
    if (idx < 0) error("Media refs: media bearer not located in carrier")
    return carrier.fieldRef(idx)?.fieldType
        ?: error("Media refs: media bearer field ref unreadable")
}

private fun shareUrlCarrierMethod(ctx: PatchRuntime): Method {
    val a = "https://www.instagram.com/p/"
    val b = "unknown"
    for (cls in ctx.bytecode.classes) {
        for (m in cls.methods) {
            if (m.indexOfFirstString(a) == null) continue
            if (m.indexOfFirstString(b) == null) continue
            return m
        }
    }
    error("Media refs: share-URL carrier method not discovered")
}

// The feed media class holds an instance field typed as the "XDTMediaDict"
// interface — the instance field whose type is an interface with the most
// zero-arg getters returning List.
private fun resolveDictField(ctx: PatchRuntime, mediaClassDef: DexClass): FieldRef {
    val mediaClass = mediaClassDef.info.descriptor
    var best: Pair<FieldRef, Int>? = null
    for (f in mediaClassDef.instanceFields) {
        if (!f.fieldType.startsWith("L")) continue
        val klass = ctx.bytecode.findClass(f.fieldType) ?: continue
        val listGetters = klass.methods.count { m -> m.info.proto == "()$LIST_TYPE" }
        if (listGetters >= 5 && (best == null || listGetters > best.second)) {
            best = FieldRef(mediaClass, f.name, f.fieldType) to listGetters
        }
    }
    return best?.first
        ?: error("Media refs: dict interface field not on $mediaClass")
}

// For each zero-arg List-returning method on the dict interface, count callers
// that follow the invoke with a check-cast to targetCastType. Winner owns the
// list. Uses the native invoke-index so we don't walk every class.
private fun resolveDictListGetter(
    ctx: PatchRuntime,
    dictType: String,
    targetCastType: String,
): MethodRef? {
    val dictClass = ctx.bytecode.findClass(dictType) ?: return null
    val lookAhead = 40
    val methodInsnsCache = HashMap<UInt, List<Instruction>>()

    var bestRef: MethodRef? = null
    var bestScore = 0

    for (candidate in dictClass.methods) {
        val info = candidate.info
        if (info.proto != "()$LIST_TYPE") continue

        val hits = findInstructionsByInvoke(dictType, info.methodName)
        if (hits.isEmpty()) continue

        var score = 0
        for (hit in hits) {
            val insns = methodInsnsCache.getOrPut(hit.method) { Method(hit.method).instructions }
            val i = hit.index.toInt()
            val end = minOf(i + lookAhead, insns.size)
            for (j in (i + 1) until end) {
                val c = insns[j]
                if (c.opcode() != Opcodes.CHECK_CAST) continue
                if (c.typeRef() == targetCastType) {
                    score++
                    break
                }
            }
        }

        if (score > bestScore) {
            bestScore = score
            bestRef = MethodRef(dictType, info.methodName, info.proto)
        }
    }

    return bestRef
}
