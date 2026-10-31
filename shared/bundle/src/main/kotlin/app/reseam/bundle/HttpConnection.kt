// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import java.net.HttpURLConnection
import java.net.URL

inline fun <T> openHttp(url: String, block: HttpURLConnection.() -> T): T {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.block()
    } finally {
        connection.disconnect()
    }
}

fun HttpURLConnection.send(body: ByteArray) {
    doOutput = true
    setFixedLengthStreamingMode(body.size)
    outputStream.use { it.write(body) }
}
