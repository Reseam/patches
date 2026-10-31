// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.web

import android.os.SystemClock
import android.util.Base64
import app.reseam.bundle.Logger
import app.reseam.bundle.isMainThread
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.minutes
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object WebPlayer {
    const val USER_AGENT = WEB_USER_AGENT
    private val playerRefreshMilliseconds = 30.minutes.inWholeMilliseconds
    private var current: WebGeneration? = null

    data class Unlocked(
        val poToken: String,
        val n: Map<String, String>,
        val sig: Map<String, String>,
    )

    data class Attestation(
        val client: JSONObject,
        val poToken: String,
        val expiresAt: Long,
        val sessionId: String,
    )

    @Synchronized
    fun warmUp() {
        generation()
    }

    fun open(deadline: Long): Session {
        check(!isMainThread()) { "The web player cannot be awaited on the main thread" }
        val generation =
            synchronized(this) {
                generation().also { it.users++ }
            }
        return try {
            Session(generation, deadline)
        } catch (exception: Exception) {
            synchronized(this) {
                generation.users--
                generation.closeIfUnused()
            }
            throw exception
        }
    }

    private fun generation(): WebGeneration {
        current
            ?.takeUnless {
                it.runtime.isClosed ||
                    SystemClock.elapsedRealtime() - it.createdAt >= playerRefreshMilliseconds
            }
            ?.let {
                return it
            }
        current?.apply {
            retired = true
            closeIfUnused()
        }
        return WebGeneration().also { current = it }
    }

    class Session
    internal constructor(internal val generation: WebGeneration, private val deadline: Long) :
        AutoCloseable {
        private var closed = false
        val signatureTimestamp = await(generation.prepared)

        // The device mints its own token; the Reseam server is the fallback when that fails.
        fun attest(videoId: String): Attestation {
            checkAwaitable()
            return try {
                val result = JSONObject(call("web.attest", JSONArray().put(videoId)))
                Attestation(
                    result.getJSONObject("client"),
                    result.getString("poToken"),
                    result.getLong("expiresAt"),
                    result.getString("sessionId"),
                )
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                throw interrupted
            } catch (exception: Exception) {
                Logger.info(exception) { "Device Web attestation failed; using the Reseam server" }
                attestOnServer(videoId)
            }
        }

        private fun attestOnServer(videoId: String): Attestation {
            val profile = JSONObject(call("web.profile", JSONArray()))
            val client = JSONObject(profile.getJSONObject("client").toString())
            val sessionId = UUID.randomUUID().toString()
            val binding =
                when (profile.getString("binding")) {
                    "none" -> return Attestation(client, "", Long.MAX_VALUE, sessionId)
                    "content" -> videoId
                    "session" -> profile.getString("sessionBinding")
                    else -> throw IllegalStateException("Unknown PoToken binding")
                }
            return mintServerToken(binding, deadline).let {
                Attestation(client, it.poToken, it.expiresAt, sessionId)
            }
        }

        fun invalidateAttestation(sessionId: String?) {
            call("web.invalidateAttestation", JSONArray().put(sessionId))
        }

        fun player(body: String, client: JSONObject): ByteArray =
            Base64.decode(
                JSONObject(call("web.player", JSONArray().put(body).put(client)))
                    .getString("response"),
                Base64.DEFAULT,
            )

        fun solveN(challenges: Collection<String>): Map<String, String> =
            solutions(
                JSONObject(
                        call(
                            "web.solve",
                            JSONArray().put(JSONObject().put("n", JSONArray(challenges))),
                        )
                    )
                    .getJSONObject("n"),
                challenges,
            )

        fun unlock(binding: String?, n: Collection<String>, sig: Collection<String>): Unlocked {
            checkAwaitable()
            val challenges = JSONObject().put("n", JSONArray(n)).put("sig", JSONArray(sig))
            val result = JSONObject(call("web.unlock", JSONArray().put(binding).put(challenges)))
            val token = result.getString("poToken")
            check(token.isNotEmpty()) { "Web player returned an empty PoToken" }
            return Unlocked(
                token,
                solutions(result.getJSONObject("n"), n),
                solutions(result.getJSONObject("sig"), sig),
            )
        }

        private fun call(function: String, arguments: JSONArray): String {
            checkAwaitable()
            val future = generation.runtime.call(function, arguments)
            try {
                return await(future)
            } finally {
                // Detach a timed-out request without cancelling shared preparation.
                future.cancel(false)
            }
        }

        internal fun <T> await(future: CompletableFuture<T>): T {
            checkAwaitable()
            try {
                val remaining = deadline - SystemClock.elapsedRealtime()
                if (remaining <= 0) throw TimeoutException("Web player request deadline exceeded")
                return future.get(remaining, TimeUnit.MILLISECONDS)
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                throw exception
            }
        }

        private fun checkAwaitable() {
            check(!closed) { "Web player session is closed" }
            check(!isMainThread()) { "The web player cannot be awaited on the main thread" }
        }

        override fun close() {
            synchronized(WebPlayer) {
                if (closed) return
                closed = true
                generation.users--
                generation.closeIfUnused()
            }
        }
    }

    private fun solutions(solved: JSONObject, expected: Collection<String>): Map<String, String> {
        val solutions = solved.keys().asSequence().associateWith(solved::getString)
        if (expected.any { solutions[it].isNullOrEmpty() })
            throw JSONException("Missing player challenge solution")
        return solutions
    }
}
