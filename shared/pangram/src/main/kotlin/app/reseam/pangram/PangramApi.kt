// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.pangram

import android.net.Uri
import app.reseam.ailabels.DetectorException
import app.reseam.ailabels.Post
import app.reseam.ailabels.PostKind
import app.reseam.bundle.jsonObject
import app.reseam.bundle.objects
import app.reseam.bundle.openHttp
import app.reseam.bundle.send
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.time.Instant
import java.util.UUID
import org.json.JSONObject

internal class PangramHttpException(val code: Int, error: String?) : IOException("Pangram returned $code: $error")

/** The browser login took too long or was used already. */
internal class LoginExpiredException : IOException("Pangram login request expired")

/** An image's bytes as its server sent them, for Pangram to keep when it no longer has the image. */
internal class ImageFile(val bytes: ByteArray, val type: String)

/** The API Pangram's browser extension uses. */
internal object PangramApi {
    private const val API = "https://web.pangram.com/api"
    private const val TIMEOUT_MS = 30_000
    private const val TOO_MANY_REQUESTS = 429

    fun predictText(session: String, post: Post): JSONObject =
        call(
            session,
            "/feed/predict",
            "text" to post.text,
            "domain" to post.domain,
            "content_type" to when (post.kind) {
                PostKind.Post -> "post"
                PostKind.Comment -> "comment"
            },
            "url" to post.url,
            "title" to post.title,
            "author_platform_id" to post.authorId,
            "author_name" to post.authorName,
            "source_date" to post.createdAtMillis?.let { Instant.ofEpochMilli(it).toString() },
        )

    /** The full result of a feed scan, with everything a repeat scan's summary leaves out. */
    fun feedResult(session: String, uuid: String): JSONObject = call(session, "/feed/$uuid").getJSONObject("response")

    /** Makes a text result public at its analysis link. */
    fun shareText(session: String, uuid: String) {
        call(session, "/chrome/share-text-query/", "uuid" to uuid)
    }

    fun account(session: String): Account = call(session, "/chrome/check-session/").getJSONObject("user").toAccount()

    /**
     * Pangram's login for clients outside the browser, made for its Word add-in: the user signs in at
     * [loginUrl] in a real browser, where every sign-in method works, and [consumeLogin] then returns the session.
     */
    fun startLogin(): String = post("/addins/word/auth-request/", jsonObject()).getString("token")

    fun loginUrl(token: String): String =
        Uri.parse("https://www.pangram.com/login").buildUpon()
            .appendQueryParameter("type", "addon")
            .appendQueryParameter("client", "m365-word")
            .appendQueryParameter("wordAuthRequest", token)
            .appendQueryParameter("addinOrigin", "https://word-addin.pangram.com")
            .build().toString()

    /** The session once the user signed in, null while Pangram still waits for them. */
    fun consumeLogin(token: String): String? =
        try {
            post("/addins/word/auth-request/consume/", jsonObject("token" to token)).optString("sessionId").ifEmpty { null }
        } catch (e: PangramHttpException) {
            throw if (e.code == HttpURLConnection.HTTP_NOT_FOUND) LoginExpiredException() else e
        }

    fun answerFeedConsent(session: String, agree: Boolean) {
        call(session, "/feed/consent", "agree" to agree)
    }

    fun imageAccess(session: String): ImageAccess = call(session, "/browser-image/config/").toImageAccess()

    fun acceptImageConsent(session: String, version: String) {
        call(session, "/browser-image/consent/", "agree" to true, "version" to version)
    }

    /** Pangram downloads [imageUrl] itself. [associationId] makes a retried submission count once. */
    fun predictImage(session: String, consentVersion: String, associationId: String, pageUrl: String, imageUrl: String): ImageOperation =
        call(
            session,
            "/browser-image/predict/",
            "association_id" to associationId,
            "intent" to "initial",
            "browser_upload" to false,
            "download_mode" to "server",
            "consent_version" to consentVersion,
            "page_url" to pageUrl,
            "image_url" to imageUrl,
        ).toImageOperation()

    /** Saves ([action] `save`) or shares ([action] `share`) the image result Pangram returned as [scanId]. */
    fun actOnImage(session: String, scanId: String, action: String, associationId: String, consentVersion: String): ImageOperation =
        call(
            session,
            "/browser-image/scans/$scanId/$action/",
            "association_id" to associationId,
            "intent" to "initial",
            "consent_version" to consentVersion,
        ).toImageOperation()

    /** Answers an action that [ImageOperation.needsUpload] with the image itself. */
    fun uploadForImageAction(session: String, scanId: String, action: String, associationId: String, consentVersion: String, operationId: String, image: ImageFile): ImageOperation {
        val fields = mapOf(
            "sessionId" to session,
            "association_id" to associationId,
            "intent" to "recovery",
            "consent_version" to consentVersion,
            "operation_id" to operationId,
        )
        val boundary = UUID.randomUUID().toString()
        val body = ByteArrayOutputStream().apply {
            fun part(headers: String, content: ByteArray) {
                write("--$boundary\r\n$headers\r\n\r\n".toByteArray())
                write(content)
                write("\r\n".toByteArray())
            }
            fields.forEach { (name, value) -> part("Content-Disposition: form-data; name=\"$name\"", value.toByteArray()) }
            part("Content-Disposition: form-data; name=\"file\"; filename=\"image\"\r\nContent-Type: ${image.type}", image.bytes)
            write("--$boundary--\r\n".toByteArray())
        }
        return post("/browser-image/scans/$scanId/$action/", body.toByteArray(), "multipart/form-data; boundary=$boundary").toImageOperation()
    }

    fun imageStatus(session: String, operationId: String): ImageOperation =
        call(session, "/browser-image/status/", "operation_ids" to listOf(operationId))
            .getJSONArray("operations").objects().single().toImageOperation()

    private fun call(session: String, path: String, vararg fields: Pair<String, Any?>): JSONObject =
        post(path, jsonObject("sessionId" to session, *fields))

    private fun post(path: String, body: JSONObject): JSONObject = post(path, body.toString().toByteArray(), "application/json")

    private fun post(path: String, body: ByteArray, type: String): JSONObject =
        openHttp(API + path) {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "POST"
            setRequestProperty("Content-Type", type)
            setRequestProperty("Origin", "moz-extension://${Session.installId}")
            send(body)
            val code = responseCode
            val text = (if (code < HttpURLConnection.HTTP_BAD_REQUEST) inputStream else errorStream)
                ?.bufferedReader()?.use { it.readText() }
            if (code < HttpURLConnection.HTTP_BAD_REQUEST && text != null) return@openHttp JSONObject(text)
            val json = text?.let { runCatching { JSONObject(it) }.getOrNull() }
            val error = json?.let { it.optString("error").ifEmpty { it.optString("reason") } }
            throw when {
                code == HttpURLConnection.HTTP_UNAUTHORIZED -> DetectorException.SignedOut()
                code == TOO_MANY_REQUESTS && error?.contains("daily", ignoreCase = true) == true -> DetectorException.DailyLimit()
                code == TOO_MANY_REQUESTS -> DetectorException.TooFast()
                else -> PangramHttpException(code, error)
            }
        }
}
