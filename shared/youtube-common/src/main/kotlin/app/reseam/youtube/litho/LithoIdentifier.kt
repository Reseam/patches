// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

import app.reseam.bundle.Logger
import app.reseam.youtube.core.debug

// All component extensions (.eml, .eml-fe, .e-b, .eml-js, e-js-b) share this prefix.
private val componentExtensionBytes = ".e".toByteArray(Charsets.US_ASCII)

internal fun identifierOf(buffer: ByteArray): String? {
    // Observed starts are <=50; bounding the search avoids scanning buffers up to 200kb.
    val lastIndex = minOf(500, buffer.size - componentExtensionBytes.size)
    var emlIndex = 0
    while (
        emlIndex <= lastIndex && !buffer.startsWith(componentExtensionBytes, emlIndex)
    ) emlIndex++
    if (emlIndex == 0 || emlIndex > lastIndex) return null
    var startIndex = emlIndex - 1
    while (startIndex > 0) {
        val character = buffer[startIndex].toInt()
        if (character in 'a'.code..'z'.code || isAsciiNumber(character) || character == '_'.code)
            startIndex--
        else {
            startIndex++
            break
        }
    }
    // Random data preceding the identifier can leave digits on its front.
    while (isAsciiNumber(buffer[startIndex].toInt())) startIndex++
    val endIndex = buffer.indexOf('|'.code.toByte(), emlIndex)
    if (endIndex < 0) {
        Logger.debug { "Could not find the identifier in the buffer" }
        return null
    }
    return String(buffer, startIndex, endIndex - startIndex, Charsets.US_ASCII)
}

private fun isAsciiNumber(character: Int): Boolean = character in '0'.code..'9'.code

private fun ByteArray.startsWith(prefix: ByteArray, at: Int): Boolean {
    for (i in prefix.indices) if (this[at + i] != prefix[i]) return false
    return true
}

private fun ByteArray.indexOf(byte: Byte, from: Int): Int {
    for (i in from until size) if (this[i] == byte) return i
    return -1
}
