// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.pangram

import app.reseam.ailabels.Checks
import app.reseam.ailabels.DetectorException
import app.reseam.ailabels.ImageVerdict
import app.reseam.ailabels.Post
import app.reseam.ailabels.PostImage
import app.reseam.ailabels.TextVerdict
import app.reseam.bundle.Logger
import app.reseam.bundle.openHttp
import app.reseam.crowdcache.sha256
import java.io.IOException
import java.net.HttpURLConnection
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

private val LIMIT_ZONE: ZoneId = ZoneId.of("America/New_York")
private const val SITE = "https://www.pangram.com"

internal object TextChecks : Checks<Post, TextVerdict>(NAME, "pangram-text", Session, LIMIT_ZONE, parallel = 2) {
    override fun key(item: Post) = "${item.domain}:${item.id}:${sha256(item.text)}"

    override fun check(item: Post): JSONObject {
        val session = Session.require()
        val result = PangramApi.predictText(session, item)
        if ((result.optJSONArray("windows")?.length() ?: 0) > 0) return result
        // A repeat scan answers with a summary; the full result fills in what it left out, such as the windows.
        val full = PangramApi.feedResult(session, result.getString("uuid"))
        full.keys().forEach { key -> if (result.isNull(key)) result.put(key, full.get(key)) }
        return result
    }

    override fun verdict(result: JSONObject) = result.toTextVerdict()

    override fun share(item: Post, verdict: TextVerdict): String {
        PangramApi.shareText(Session.require(), verdict.id)
        return fullAnalysis(item, verdict)
    }

    override fun fullAnalysis(item: Post, verdict: TextVerdict) = "$SITE/feed/${verdict.id}"
}

internal object ImageChecks : Checks<PostImage, ImageVerdict>(NAME, "pangram-image", Session, LIMIT_ZONE, parallel = 4) {
    private const val TIMEOUT_MS = 90_000L
    private const val POLL_MS = 1_000L

    private val access = ConcurrentHashMap<String, ImageAccess>()

    override fun key(item: PostImage) = "${item.domain}:${sha256(item.url)}"

    override fun check(item: PostImage): JSONObject {
        val session = Session.require()
        val access = access(session)
        if (!access.available) throw IOException("Pangram image checks are unavailable for this account")
        if (!access.consentAccepted) throw DetectorException.NeedsConsent()
        val association = sha256("${Session.installId}:${item.url}")
        val operation = awaitOperation(session, PangramApi.predictImage(session, access.consentVersion, association, item.pageUrl, item.url))
        return operation.result ?: throw IOException("Pangram image check ${operation.state}: ${operation.reason}")
    }

    override fun verdict(result: JSONObject) = result.toImageVerdict()

    override fun share(item: PostImage, verdict: ImageVerdict): String =
        SITE + (act(item, verdict, "share").sharePath ?: throw IOException("Pangram did not share the image result"))

    override fun fullAnalysis(item: PostImage, verdict: ImageVerdict): String =
        SITE + (act(item, verdict, "save").ownerPath ?: throw IOException("Pangram did not save the image result"))

    /** Accepts Pangram's terms for image checks. */
    fun agree() {
        val session = Session.id ?: return
        runCatching {
            PangramApi.acceptImageConsent(session, access(session).consentVersion)
            access[session] = PangramApi.imageAccess(session)
        }
            .onSuccess { consentGiven() }
            .onFailure { Logger.warn(it) { "Accepting Pangram's image terms failed" } }
    }

    private fun access(session: String): ImageAccess = access.getOrPut(session) { PangramApi.imageAccess(session) }

    /** Saves or shares an image result. Pangram keeps no copy of a checked image, so the first action uploads it. */
    private fun act(image: PostImage, verdict: ImageVerdict, action: String): ImageOperation {
        val session = Session.require()
        val access = access(session)
        val association = sha256("${Session.installId}:${verdict.id}:$action:${UUID.randomUUID()}")
        var operation = PangramApi.actOnImage(session, verdict.id, action, association, access.consentVersion)
        if (operation.needsUpload) {
            val file = download(image.url, access.maxUploadBytes)
            operation = PangramApi.uploadForImageAction(session, verdict.id, action, association, access.consentVersion, operation.id, file)
        }
        operation = awaitOperation(session, operation)
        if (operation.state != "succeeded") throw IOException("Pangram image $action ${operation.state}: ${operation.reason}")
        return operation
    }

    private fun awaitOperation(session: String, started: ImageOperation): ImageOperation {
        var operation = started
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!operation.finished) {
            if (System.currentTimeMillis() > deadline) throw IOException("Pangram image ${operation.id} timed out")
            Thread.sleep(POLL_MS)
            operation = PangramApi.imageStatus(session, operation.id)
        }
        return operation
    }

    private fun download(url: String, limit: Long): ImageFile =
        openHttp(url) {
            if (responseCode != HttpURLConnection.HTTP_OK) throw IOException("Image download returned $responseCode")
            if (contentLengthLong > limit) throw IOException("Image is larger than Pangram takes")
            ImageFile(inputStream.use { it.readBytes() }, contentType)
        }
}
