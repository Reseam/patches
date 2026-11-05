// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("WithheldPosts")

package app.reseam.x.withheld

import android.content.Context
import app.reseam.bundle.Logger
import app.reseam.bundle.showToast
import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject

private const val REPOST_OPERATION = "CreateRepostMutation"
private const val STATUS_NOT_FOUND = 144

@Volatile private var appContext: Context? = null

@Export
fun init(context: Context) {
    appContext = context.applicationContext
}

@Export
fun install(builder: OkHttpClient.Builder?) {
    if (builder == null || builder.interceptors().any { it is WithheldInterceptor }) return
    builder.addInterceptor(WithheldInterceptor)
}

private object WithheldInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!XSettings.showWithheldPosts.value || !request.url.isGraphql || !response.isJson) return response
        val type = response.body.contentType()
        val text = response.body.string()
        val operation = request.url.pathSegments.last()
        val rewritten =
            runCatching {
                noticeRefusedRepost(operation, text)
                WithheldRewriter.rewrite(text, operation, request.url.queryParameter("variables"))
            }
                .onFailure { Logger.error(it) { "Could not fill withheld posts in $operation" } }
                .getOrNull()
        return response.newBuilder().body((rewritten ?: text).toResponseBody(type)).build()
    }

    private val HttpUrl.isGraphql: Boolean
        get() = host == "api.x.com" && encodedPath.startsWith("/graphql/")

    private val Response.isJson: Boolean
        get() = code == 200 && body.contentType()?.subtype == "json"

    // X resolves a repost under the viewer's country, so a withheld post is "not found" there
    // while likes, bookmarks, replies and quotes of it go through. X itself shows nothing.
    private fun noticeRefusedRepost(operation: String, text: String) {
        if (operation != REPOST_OPERATION) return
        val errors = JSONObject(text).optJSONArray("errors")?.objects().orEmpty()
        if (errors.any { it.optInt("code") == STATUS_NOT_FOUND }) appContext?.showToast(XStrings.withheldRepostRefused)
    }
}
