// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.web

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.webkit.JavascriptInterface
import android.webkit.WebView
import app.reseam.bundle.Logger
import app.reseam.bundle.mainThreadHandler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray

// Each renderer owns its bridge; replies from retired renderers cannot reach replacement calls.
internal class WebRuntime(context: Context) {
    private val main = mainThreadHandler()
    private val ready = CompletableFuture<Void>()
    private val calls = ConcurrentHashMap<Int, CompletableFuture<String>>()
    private val nextId = AtomicInteger()
    private val failure = AtomicReference<Throwable?>()
    private var view: WebView? = null

    init {
        val startupTimeout = Runnable { close(TimeoutException("Web runtime startup timed out")) }
        main.postDelayed(startupTimeout, TIMEOUT_MILLISECONDS)
        ready.whenComplete { _, _ -> main.removeCallbacks(startupTimeout) }
        main.post { start(context) }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun start(context: Context) {
        if (isClosed) return
        try {
            val webView = WebView(context)
            view = webView
            webView.apply {
                settings.javaScriptEnabled = true
                settings.userAgentString = WEB_USER_AGENT
                addJavascriptInterface(Bridge(), "reseamHost")
                webViewClient = RuntimePage(context, ::close)
                loadUrl("https://$WEB_HOST${WEB_ROOT}index.html")
            }
        } catch (exception: RuntimeException) {
            close(exception)
        }
    }

    fun call(function: String, arguments: JSONArray): CompletableFuture<String> {
        val id = nextId.incrementAndGet()
        val result = CompletableFuture<String>()
        calls[id] = result
        val timeout = Runnable {
            result.completeExceptionally(TimeoutException("Web runtime call timed out: $function"))
        }
        main.postDelayed(timeout, TIMEOUT_MILLISECONDS)
        result.whenComplete { _, _ ->
            calls.remove(id, result)
            main.removeCallbacks(timeout)
        }
        val error = failure.get()
        if (error != null) result.completeExceptionally(error)
        else
            ready.whenComplete { _, startupError ->
                if (startupError != null) result.completeExceptionally(startupError)
                else main.post { evaluate(id, function, arguments, result) }
            }
        return result
    }

    private fun evaluate(
        id: Int,
        function: String,
        arguments: JSONArray,
        result: CompletableFuture<String>,
    ) {
        if (result.isDone) return
        val closed = failure.get()
        if (closed != null) {
            result.completeExceptionally(closed)
            return
        }
        try {
            checkNotNull(view) { "A ready runtime has a WebView" }
                .evaluateJavascript("host.call($id, $function, $arguments)", null)
        } catch (exception: RuntimeException) {
            close(exception)
        }
    }

    val isClosed: Boolean
        get() = failure.get() != null

    fun close(error: Throwable) {
        if (!failure.compareAndSet(null, error)) return
        ready.completeExceptionally(error)
        calls.values.forEach { it.completeExceptionally(error) }
        main.post {
            view?.let { webView ->
                webView.removeJavascriptInterface("reseamHost")
                webView.destroy()
                view = null
            }
        }
    }

    private inner class Bridge {
        @JavascriptInterface fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()

        @JavascriptInterface
        fun ready() {
            ready.complete(null)
        }

        @JavascriptInterface
        fun fail(error: String) {
            Logger.error { "Web runtime failed: $error" }
            close(IllegalStateException(error))
        }

        @JavascriptInterface
        fun resolve(id: Int, json: String) {
            calls[id]?.complete(json)
        }

        @JavascriptInterface
        fun reject(id: Int, error: String) {
            calls[id]?.completeExceptionally(IllegalStateException(error))
        }
    }
}

internal const val WEB_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

private const val TIMEOUT_MILLISECONDS = 20_000L
