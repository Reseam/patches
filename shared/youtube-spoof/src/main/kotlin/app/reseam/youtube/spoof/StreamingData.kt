// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.net.Uri
import android.os.SystemClock
import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto

@Export
class StreamingData
private constructor(
    internal val response: ByteArray,
    internal val client: ClientType,
    internal val expiresAt: Long,
    internal val sabr: SabrData?,
    internal val playerResponse: ByteArray?,
    internal val native: Boolean = false,
) {
    internal constructor(
        client: ClientType,
    ) : this(ByteArray(0), client, Long.MAX_VALUE, null, null, native = true)

    internal constructor(
        streamingData: ByteArray,
        client: ClientType,
        playerResponse: ByteArray?,
    ) : this(
        withStreamingData(Proto.replace(streamingData, mapOf(15 to null))),
        client,
        streamUrls(streamingData).minOfOrNull { streamExpiresAt(Uri.parse(it.url)) } ?: 0,
        null,
        playerResponse,
    )

    internal constructor(
        sabr: SabrData,
        playerResponse: ByteArray?,
    ) : this(
        withStreamingData(sabr.streams),
        sabr.client,
        streamExpiresAt(sabr.uri),
        sabr,
        playerResponse,
    )

    internal val isFresh: Boolean
        get() =
            expiresAt > System.currentTimeMillis() + 60_000 &&
                (sabr == null ||
                    sabr.attestation.expiresAt > SystemClock.elapsedRealtime() + 30_000)
}
