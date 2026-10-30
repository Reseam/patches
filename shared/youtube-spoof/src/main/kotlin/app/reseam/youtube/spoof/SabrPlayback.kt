// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SabrPlayback")

package app.reseam.youtube.spoof

import android.os.SystemClock
import android.util.Base64
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.debug
import app.reseam.youtube.web.WebPlayer
import java.nio.ByteBuffer
import org.json.JSONObject

// Native protobuf, transport and token-manager owners are associated by weak identity.
private const val REFRESH_MARGIN_MILLISECONDS = 30_000L
private val sabrStreams = WeakIdentityMap<SabrData>()
private val transports = WeakIdentityMap<SabrSession>()
private val sabrTokens = WeakIdentityMap<SabrSession>()
private val responses = WeakIdentityMap<SabrResponse>()

@Export
fun installed(nativeStreams: Any?, streams: StreamingData) {
    val sabr = streams.sabr
    if (sabr != null) sabrStreams.put(nativeStreams, sabr) else sabrStreams.remove(nativeStreams)
}

@Export
fun prepare(streams: Any?, config: Any?, transport: Any?, tokens: Any?): Any? {
    val data = sabrStreams[streams]
    if (data == null) {
        transports.remove(transport)
        sabrTokens.remove(tokens)
        return config
    }
    try {
        val replacement = replaceConfig(config, data.nativeCommonConfig)
        val session = SabrSession(data)
        transports.put(transport, session)
        sabrTokens.put(tokens, session)
        Logger.debug { "Native ${data.client} SABR playback created for ${data.videoId}" }
        return replacement
    } catch (exception: Exception) {
        transports.remove(transport)
        sabrTokens.remove(tokens)
        Logger.error(exception) { "Could not prepare native SABR playback" }
        return config
    }
}

@Export
fun token(owner: Any?, original: ByteArray?): ByteArray? {
    val session = sabrTokens[owner] ?: transports[owner]
    return try {
        if (session == null) original else decodeToken(session.attestation)
    } catch (exception: IllegalArgumentException) {
        Logger.error(exception) { "Invalid SABR token encoding" }
        original
    }
}

@Export
fun refresh(manager: Any?, callback: Any?): Boolean {
    val session = sabrTokens[manager] ?: return false
    session.refresh(session.rejectedSession).whenComplete { attestation, error ->
        if (error != null) Logger.error(error) { "SABR token refresh failed" }
        if (callback != null) {
            try {
                tokenMinted(callback, if (error == null) decodeToken(attestation) else ByteArray(0))
            } catch (exception: Exception) {
                Logger.error(exception) { "SABR token callback failed" }
            }
        }
    }
    return true
}

@Export
fun request(transport: Any?, callback: Any?, original: ByteArray?): ByteArray? {
    val session = transports[transport]
    if (session == null || original == null) return original
    try {
        // VideoPlaybackAbrRequest.streamerContext (19): clientInfo (1), poToken (2).
        val context = Proto.bytes(original, 19) ?: return original
        val attestation = session.attestation
        if (attestation.expiresAt - SystemClock.elapsedRealtime() < REFRESH_MARGIN_MILLISECONDS) {
            session.refresh(attestation.sessionId)
        }
        val rewritten =
            Proto.replace(
                context,
                mapOf(
                    1 to clientInfo(session.data.client, attestation.client),
                    2 to decodeToken(attestation),
                ),
            )
        val body = Proto.replace(original, mapOf(19 to session.contexts.append(rewritten)))
        responses.put(
            callback,
            SabrResponse { type, message -> session.control(type, message, attestation.sessionId) },
        )
        return body
    } catch (exception: Exception) {
        Logger.error(exception) { "Could not adapt SABR request" }
        return original
    }
}

@Export
fun response(callback: Any?, buffer: ByteBuffer?) {
    val response = responses[callback]
    if (response == null || buffer == null) return
    try {
        response.read(buffer)
    } catch (exception: Exception) {
        responses.remove(callback)
        Logger.error(exception) { "Invalid SABR control response" }
    }
}

@Export
fun completed(callback: Any?) {
    responses.remove(callback)
}

private fun decodeToken(attestation: WebPlayer.Attestation): ByteArray =
    Base64.decode(attestation.poToken, Base64.URL_SAFE)

// ClientInfo: clientName (16), clientVersion (17), hl (21), gl (22).
private fun clientInfo(type: ClientType, client: JSONObject): ByteArray =
    Proto.Writer()
        .number(16, type.clientId.toLong())
        .string(17, client.getString("clientVersion"))
        .string(21, client.optString("hl", "en"))
        .string(22, client.optString("gl", "US"))
        .toByteArray()

// The SABR patch supplies the app's retained configuration and protobuf types.
@Export external fun replaceConfig(original: Any?, common: Any?): Any

// Parse using the native extension registry before publishing the replacement.
@Export external fun parseCommonConfig(common: ByteArray): Any

// The SABR patch invokes the app's native PoToken callback interface.
@Export fun tokenMinted(callback: Any, token: ByteArray) {}
