// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.returnType
import app.reseam.patch.method
import app.reseam.patch.point

/** The response builder method whose owner is the regular video-ID class. */
val videoIdParentMethod = method("videoIdParentMethod") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    paramCount(1)
    literals(524288)
    custom {
        returnType.startsWith("[L") && parameterTypes.singleOrNull()?.startsWith("L") == true
    }
}

private val videoIdParentClass = classTarget("videoIdParentClass") {
    bytecode.findClass(videoIdParentMethod.owner) ?: error("The video-ID parent class is missing")
}

/** The child method that extracts the ID and puts it into the response map. */
val videoIdMethod = method("videoIdMethod") {
    inClass(videoIdParentClass)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    paramCount(1)
    calls { returns(Type.String); params() }
    custom { parameterTypes.singleOrNull()?.startsWith("L") == true }
}

/** The player response's video-ID getter, an interface method shared by the player models. */
val videoIdGetter = videoIdMethod.point {
    invokeInterface { params(); returns(Type.String) }
}.callee()
