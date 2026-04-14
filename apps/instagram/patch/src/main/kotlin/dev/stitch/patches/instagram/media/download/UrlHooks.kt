/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.patches.instagram.media.download

import dev.stitch.patch.DexClass
import dev.stitch.patch.FieldRef
import dev.stitch.patch.Instruction
import dev.stitch.patch.Method
import dev.stitch.patch.MethodRef
import dev.stitch.patch.Opcodes
import dev.stitch.patch.PatchRuntime
import dev.stitch.patch.buildInstructions
import dev.stitch.patch.findInstructionsByInvoke
import dev.stitch.patch.methodRef
import dev.stitch.patch.opcode
import dev.stitch.patch.typeRef

private const val MEDIA_META = "Ldev/stitch/instagram/download/MediaMeta;"
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
    bridge.setRegisters(registersSize = 2, outsSize = 1)
    bridge.setInstructions(buildInstructions {
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
    bridge.setRegisters(registersSize = 3, outsSize = 2)
    bridge.setInstructions(buildInstructions {
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
    bridge.setRegisters(registersSize = 2, outsSize = 1)
    bridge.setInstructions(buildInstructions {
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
