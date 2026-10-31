// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * Reads without closing this stream, throwing IOException([message]) above [limit] bytes.
 * [checkDeadline] runs after each successful read, before accepting the chunk.
 */
inline fun InputStream.readBounded(
    limit: Int,
    bufferSize: Int,
    message: String,
    checkDeadline: () -> Unit = {},
): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(bufferSize)
    while (true) {
        val count = read(buffer)
        if (count == -1) return output.toByteArray()
        checkDeadline()
        if (output.size() + count > limit) throw IOException(message)
        output.write(buffer, 0, count)
    }
}

/** Reads bounded UTF-8 text and closes the stream; an absent error stream yields empty text. */
fun HttpURLConnection.readResponseText(
    limit: Int,
    message: String,
    bufferSize: Int = 4096,
    errorBody: Boolean = false,
): String =
    (if (errorBody) errorStream else inputStream).use { input ->
        input?.readBounded(limit, bufferSize, message)?.toString(Charsets.UTF_8).orEmpty()
    }
