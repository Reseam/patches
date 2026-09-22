// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method

/**
 * Every image the app loads goes through one request object, built from the URL string. Patches
 * that rewrite an image URL hook its constructor; the class is found through the only method that
 * names the set of characters the URL is escaped against.
 */
val imageUrlEscapeCharacters = method("imageUrlEscapeCharacters") {
    strings("@#&=*+-_.,:!?()/~'%;\$[]")
    returns(Type.String)
    params()
}

/** The delegating one-argument constructor also reaches this hook. Cached and shared by users. */
val imageRequestConstructor = method("imageRequestConstructor") {
    inClass(klass(imageUrlEscapeCharacters.owner))
    flags(AccessFlags.CONSTRUCTOR)
    paramCount(2)
    param(0, Type.String)
}
