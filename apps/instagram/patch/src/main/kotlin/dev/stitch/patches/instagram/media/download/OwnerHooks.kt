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
import dev.stitch.patch.Method
import dev.stitch.patch.PatchRuntime
import dev.stitch.patch.buildInstructions

private const val MEDIA_META = "Ldev/stitch/instagram/download/MediaMeta;"
private const val ACCESS_FINAL = 0x10
private const val ACCESS_STATIC = 0x08

// Preference order when finding a Context-assignable field on story owner.
// We intentionally only walk these known descriptors rather than computing
// full class-hierarchy assignability (too expensive and fragile for non-AOSP
// framework types).
private val CONTEXT_COMPATIBLE_TYPES = listOf(
    "Landroid/app/Activity;",
    "Landroidx/fragment/app/FragmentActivity;",
    "Landroid/content/Context;",
)

internal fun hookOwnerBridges(ctx: PatchRuntime) {
    rewriteReelItemMedia(ctx)
    rewriteStoryOwnerReelItem(ctx)
    rewriteStoryOwnerContext(ctx)
}

// ReelItem's media field is the single instance-final LX/4xa field. The
// ambiguity between multiple (non-final) LX/4xa fields is resolved by the
// FINAL flag — the "canonical" media is the Kotlin `val`.
private fun rewriteReelItemMedia(ctx: PatchRuntime) {
    val reelItemClass = REEL_ITEM_TYPE
    val classDef = ctx.bytecode.findClass(reelItemClass)
        ?: error("ReelItem class not found")

    val mediaType = feedMediaType()
    val field = classDef.instanceFields
        .filter { it.fieldType == mediaType && isInstanceFinal(it.accessFlags.toInt()) }
        .firstOrNull()
        ?: error("ReelItem media field (final $mediaType) not found")

    val bridge = findBridge(ctx, "reelItemMedia")
    bridge.setRegisters(registersSize = 2, outsSize = 0)
    bridge.setInstructions(buildInstructions {
        checkCast(1, reelItemClass)
        igetObject(0, 1, FieldRef(reelItemClass, field.name, field.fieldType))
        returnObject(0)
    })
    ctx.log.info("Bound MediaMeta.reelItemMedia -> $reelItemClass.${field.name}")
}

private fun rewriteStoryOwnerReelItem(ctx: PatchRuntime) {
    val storyOwnerClass = storyActionSheetClassFingerprint.method.info.classDescriptor
    val classDef = ctx.bytecode.findClass(storyOwnerClass)
        ?: error("Story owner class not found: $storyOwnerClass")

    val field = classDef.instanceFields
        .firstOrNull { it.fieldType == REEL_ITEM_TYPE && !isStatic(it.accessFlags.toInt()) }
        ?: error("ReelItem field not found on story owner $storyOwnerClass")

    val bridge = findBridge(ctx, "storyOwnerReelItem")
    bridge.setRegisters(registersSize = 2, outsSize = 0)
    bridge.setInstructions(buildInstructions {
        checkCast(1, storyOwnerClass)
        igetObject(0, 1, FieldRef(storyOwnerClass, field.name, field.fieldType))
        returnObject(0)
    })
    ctx.log.info("Bound MediaMeta.storyOwnerReelItem -> $storyOwnerClass.${field.name}")
}

private fun rewriteStoryOwnerContext(ctx: PatchRuntime) {
    val storyOwnerClass = storyActionSheetClassFingerprint.method.info.classDescriptor
    val classDef = ctx.bytecode.findClass(storyOwnerClass)
        ?: error("Story owner class not found: $storyOwnerClass")

    val field = CONTEXT_COMPATIBLE_TYPES.firstNotNullOfOrNull { type ->
        classDef.instanceFields.firstOrNull { it.fieldType == type && !isStatic(it.accessFlags.toInt()) }
    } ?: error("Context field not found on story owner $storyOwnerClass")

    val bridge = findBridge(ctx, "storyOwnerContext")
    bridge.setRegisters(registersSize = 2, outsSize = 0)
    bridge.setInstructions(buildInstructions {
        checkCast(1, storyOwnerClass)
        igetObject(0, 1, FieldRef(storyOwnerClass, field.name, field.fieldType))
        returnObject(0)
    })
    ctx.log.info("Bound MediaMeta.storyOwnerContext -> $storyOwnerClass.${field.name}:${field.fieldType}")
}

private fun isInstanceFinal(flags: Int): Boolean =
    (flags and ACCESS_FINAL) != 0 && (flags and ACCESS_STATIC) == 0

private fun isStatic(flags: Int): Boolean = (flags and ACCESS_STATIC) != 0

private fun findBridge(ctx: PatchRuntime, name: String): Method =
    ctx.bytecode.findClass(MEDIA_META)
        ?.methods
        ?.firstOrNull { it.info.methodName == name }
        ?: error("MediaMeta.$name bridge not found")
