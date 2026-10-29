// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.refs

import app.reseam.patch.CodeScope
import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point

const val ORIGINAL_SIZE = "orig"

private val imageSizes = klass("imageSizes") { strings("pin.images[", "1200x") }

private val closeupSize = imageSizes.method("<init>").point { string("736x") }.next { opcode(Opcode.IPUT_OBJECT) }.field("closeupSize")

val requestedImageSizes = method("requestedImageSizes") {
    inClass(imageSizes)
    returns("java.util.HashSet")
}

val closeupImageSizes = methods("closeupImageSizes") {
    inClass(imageSizes)
    accesses { owner(closeupSize.owner); name(closeupSize.name) }
    params()
    returns(Type.String)
}

/** Adds the original size to the sizes [requestedImageSizes] returns. */
fun CodeScope.requestOriginalSize() {
    result.callVirtual("java.util.HashSet", "add", "(Ljava/lang/Object;)Z", string(ORIGINAL_SIZE))
}
