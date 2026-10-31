// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.web

import android.content.Context
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.IOException

internal class RuntimePage(private val context: Context, private val close: (Throwable) -> Unit) :
    WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url
        val path = url.path
        if (url.host != WEB_HOST || path == null || !path.startsWith(WEB_ROOT)) return null
        return try {
            WebResourceResponse(
                if (path.endsWith(".html")) "text/html" else "text/javascript",
                "utf-8",
                context.assets.open(ASSETS + path.substring(WEB_ROOT.length)),
            )
        } catch (exception: IOException) {
            close(exception)
            WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null, null)
        }
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (request.isForMainFrame)
            close(IOException("Web runtime load failed: ${error.errorCode}"))
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        response: WebResourceResponse,
    ) {
        if (request.isForMainFrame) close(IOException("Web runtime HTTP ${response.statusCode}"))
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        close(IllegalStateException("Web runtime renderer exited, crashed: ${detail.didCrash()}"))
        return true
    }
}

internal const val WEB_HOST = "www.youtube.com"
internal const val WEB_ROOT = "/reseam/web/"
private const val ASSETS = "reseam-patches/web/"
