// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("XApi")

package app.reseam.x.api

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

private const val API_HOST = "api.x.com"
private val JSON = "application/json".toMediaType()
private val rewriters = CopyOnWriteArrayList<GraphqlRewriter>()
private val responders = CopyOnWriteArrayList<ApiResponder>()

fun register(rewriter: GraphqlRewriter) {
    rewriters.addIfAbsent(rewriter)
}

fun register(responder: ApiResponder) {
    responders.addIfAbsent(responder)
}

@Export
fun install(builder: OkHttpClient.Builder?) {
    if (builder == null || builder.interceptors().any { it is XApiInterceptor }) return
    builder.addInterceptor(XApiInterceptor)
}

/** The bytes a request sends. Filled in by the patch, since it writes them through the app's own okio. */
@Export
external fun requestBytes(body: RequestBody): ByteArray

private object XApiInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != API_HOST) return chain.proceed(request)
        return answer(request) ?: rewrite(request, chain.proceed(request))
    }

    private fun answer(request: Request): Response? {
        val path = request.url.encodedPath
        val enabled = responders.filter { it.isEnabled && it.path == path }
        if (enabled.isEmpty()) return null
        val apiRequest = ApiRequest(path) { request.body?.let(::requestBytes) }
        val body = enabled.firstNotNullOfOrNull { it.tryRespond(apiRequest) } ?: return null
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody(JSON))
            .build()
    }

    private fun rewrite(request: Request, response: Response): Response {
        val enabled = rewriters.filter { it.isEnabled }
        if (enabled.isEmpty() || !request.url.isGraphql || !response.isJson) return response
        val type = response.body.contentType()
        val url = request.url
        val graphql = GraphqlResponse(url.pathSegments.last(), url.queryParameter("variables"), response.body.string())
        val changed = enabled.map { rewriter -> rewriter.tryRewrite(graphql) }.any { it }
        val body = if (changed) graphql.json.toString() else graphql.body
        return response.newBuilder().body(body.toResponseBody(type)).build()
    }

    private fun GraphqlRewriter.tryRewrite(response: GraphqlResponse): Boolean =
        runCatching { rewrite(response) }
            .onFailure { Logger.error(it) { "${this::class.simpleName} failed on ${response.operation}" } }
            .getOrDefault(false)

    private fun ApiResponder.tryRespond(request: ApiRequest): String? =
        runCatching { respond(request) }
            .onFailure { Logger.error(it) { "${this::class.simpleName} failed on ${request.path}" } }
            .getOrNull()

    private val HttpUrl.isGraphql: Boolean
        get() = encodedPath.startsWith("/graphql/")

    private val Response.isJson: Boolean
        get() = code == 200 && body.contentType()?.subtype == "json"
}
