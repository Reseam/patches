// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media.download

import app.reseam.patch.DexClass
import app.reseam.patch.FieldRef
import app.reseam.patch.Instruction
import app.reseam.patch.Method
import app.reseam.patch.MethodRef
import app.reseam.patch.Opcodes
import app.reseam.patch.PatchRuntime
import app.reseam.patch.buildInstructions
import app.reseam.patch.findInstructionsByInvoke
import app.reseam.patch.methodRef
import app.reseam.patch.opcode
import app.reseam.patch.typeRef

private const val MEDIA_META = "Lapp/reseam/instagram/download/MediaMeta;"
private const val LIST_TYPE = "Ljava/util/List;"

internal fun hookUrlBridges(ctx: PatchRuntime) {
    val mediaClass = feedMediaType()
    val mediaClassDef = ctx.bytecode.findClass(mediaClass)
        ?: error("feed media class not found: $mediaClass")

    rewriteImageUrlBridge(ctx, mediaClass, mediaClassDef)

    val dictField = resolveDictField(ctx, mediaClassDef)
    val videoGetter = resolveDictListGetter(ctx, dictField.fieldType, VIDEO_VERSION_INTF_TYPE)
        ?: error("Could not correlate video_versions getter on ${dictField.fieldType}")
    val carouselGetter = resolveDictListGetter(ctx, dictField.fieldType, mediaClass)
        ?: error("Could not correlate carousel_media getter on ${dictField.fieldType}")

    rewriteVideoUrlBridge(ctx, mediaClass, dictField, videoGetter)
    rewriteCarouselChildrenBridge(ctx, mediaClass, dictField, carouselGetter)
}

private fun rewriteImageUrlBridge(ctx: PatchRuntime, mediaClass: String, mediaClassDef: DexClass) {
    val imageField = mediaClassDef.instanceFields.firstOrNull { it.fieldType == EXTENDED_IMAGE_URL_TYPE }
        ?: error("ExtendedImageUrl field not found on $mediaClass")

    val bridge = findBridge(ctx, "imageUrl")
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
    ctx.log.info("Bound MediaMeta.imageUrl -> $mediaClass.${imageField.name}")
}

private fun rewriteVideoUrlBridge(
    ctx: PatchRuntime,
    mediaClass: String,
    dictField: FieldRef,
    videoGetter: MethodRef,
) {
    val bridge = findBridge(ctx, "videoUrl")
    bridge.replaceBody(registersSize = 3, outsSize = 2, insns = buildInstructions {
        // v2 = p0 (Object media)
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
    ctx.log.info("Bound MediaMeta.videoUrl -> ${dictField.fieldType}->${videoGetter.name}")
}

private fun rewriteCarouselChildrenBridge(
    ctx: PatchRuntime,
    mediaClass: String,
    dictField: FieldRef,
    carouselGetter: MethodRef,
) {
    val bridge = findBridge(ctx, "carouselChildren")
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
    ctx.log.info("Bound MediaMeta.carouselChildren -> ${dictField.fieldType}->${carouselGetter.name}")
}

private fun findBridge(ctx: PatchRuntime, name: String): Method =
    ctx.bytecode.findClass(MEDIA_META)
        ?.methods
        ?.firstOrNull { it.info.methodName == name }
        ?: error("MediaMeta.$name bridge not found")

// The feed media class holds an instance field typed as the "XDTMediaDict"
// interface. Identify it as the instance field whose type is an interface class
// with the most zero-arg getters returning List. Avoids hardcoding the
// obfuscated descriptor (e.g. LX/9kQ;) from a specific IG build.
private fun resolveDictField(ctx: PatchRuntime, mediaClassDef: DexClass): FieldRef {
    val mediaClass = mediaClassDef.info.descriptor
    var best: Pair<FieldRef, Int>? = null
    for (f in mediaClassDef.instanceFields) {
        if (!f.fieldType.startsWith("L")) continue
        val klass = ctx.bytecode.findClass(f.fieldType) ?: continue
        val listGetters = klass.methods.count { m ->
            m.info.proto == "()$LIST_TYPE"
        }
        if (listGetters >= 5 && (best == null || listGetters > best.second)) {
            best = FieldRef(mediaClass, f.name, f.fieldType) to listGetters
        }
    }
    return best?.first
        ?: error("dict interface field not found on $mediaClass")
}

// For each zero-arg List-returning method on the dict interface, count how
// many call sites are followed (within lookAhead) by a check-cast to
// targetCastType. The winner is the getter for that list. Uses the native
// invoke-index so it doesn't have to walk every class in the APK.
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
