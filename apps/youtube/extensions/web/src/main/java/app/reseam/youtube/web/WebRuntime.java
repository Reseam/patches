// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.web;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import app.reseam.youtube.core.Logger;

/**
 * One hidden WebView at the YouTube origin. Calls have a bounded lifetime, including startup.
 * A fatal error retires this instance and fails every waiter; a new instance owns a new bridge,
 * so callbacks from a destroyed renderer cannot complete calls in its replacement.
 */
final class WebRuntime {
    private static final String HOST = "www.youtube.com";
    private static final String ROOT = "/reseam/web/";
    private static final String ASSETS = "reseam/web/";
    private static final long TIMEOUT_MILLISECONDS = 20_000;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final CompletableFuture<Void> ready = new CompletableFuture<>();
    private final Map<Integer, CompletableFuture<String>> calls = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    /** Accessed only on the main thread. */
    private WebView view;

    WebRuntime(Context context) {
        Runnable startupTimeout = () -> close(new TimeoutException("Web runtime startup timed out"));
        main.postDelayed(startupTimeout, TIMEOUT_MILLISECONDS);
        ready.whenComplete((unused, error) -> main.removeCallbacks(startupTimeout));
        main.post(() -> start(context));
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void start(Context context) {
        if (isClosed()) return;
        try {
            view = new WebView(context);
            view.getSettings().setJavaScriptEnabled(true);
            view.addJavascriptInterface(new Bridge(), "reseamHost");
            view.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    Uri url = request.getUrl();
                    String path = url.getPath();
                    if (!HOST.equals(url.getHost()) || path == null || !path.startsWith(ROOT)) return null;
                    try {
                        return new WebResourceResponse(path.endsWith(".html") ? "text/html" : "text/javascript", "utf-8",
                                context.getAssets().open(ASSETS + path.substring(ROOT.length())));
                    } catch (IOException exception) {
                        close(exception);
                        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null, null);
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request.isForMainFrame()) close(new IOException("Web runtime load failed: " + error.getErrorCode()));
                }

                @Override
                public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                    if (request.isForMainFrame()) close(new IOException("Web runtime HTTP " + response.getStatusCode()));
                }

                @Override
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    close(new IllegalStateException("Web runtime renderer exited, crashed: " + detail.didCrash()));
                    return true;
                }
            });
            view.loadUrl("https://" + HOST + ROOT + "index.html");
        } catch (RuntimeException exception) {
            close(exception);
        }
    }

    /**
     * Calls an async function and completes with JSON. A call's timeout, rejection or cancellation
     * affects only that call. Renderer, startup and worker failures use the separate fatal path.
     */
    CompletableFuture<String> call(String function, JSONArray arguments) {
        int id = nextId.incrementAndGet();
        CompletableFuture<String> result = new CompletableFuture<>();
        calls.put(id, result);
        Runnable timeout = () -> result.completeExceptionally(new TimeoutException("Web runtime call timed out: " + function));
        main.postDelayed(timeout, TIMEOUT_MILLISECONDS);
        result.whenComplete((value, error) -> {
            calls.remove(id, result);
            main.removeCallbacks(timeout);
        });
        Throwable error = failure.get();
        if (error != null) result.completeExceptionally(error);
        else ready.whenComplete((unused, startupError) -> {
            if (startupError != null) result.completeExceptionally(startupError);
            else main.post(() -> {
                if (result.isDone()) return;
                Throwable closed = failure.get();
                if (closed != null) {
                    result.completeExceptionally(closed);
                    return;
                }
                try {
                    view.evaluateJavascript("host.call(" + id + ", " + function + ", " + arguments + ")", null);
                } catch (RuntimeException exception) {
                    close(exception);
                }
            });
        });
        return result;
    }

    boolean isClosed() {
        return failure.get() != null;
    }

    /** May run on any thread. Closing is idempotent and releases every pending call immediately. */
    void close(Throwable error) {
        if (!failure.compareAndSet(null, error)) return;
        ready.completeExceptionally(error);
        calls.values().forEach(result -> result.completeExceptionally(error));
        main.post(() -> {
            if (view == null) return;
            view.removeJavascriptInterface("reseamHost");
            view.destroy();
            view = null;
        });
    }

    private final class Bridge {
        /** Monotonic milliseconds including device sleep, for server-issued token lifetimes. */
        @JavascriptInterface
        public long elapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

        @JavascriptInterface
        public void ready() {
            ready.complete(null);
        }

        @JavascriptInterface
        public void fail(String error) {
            Logger.error(() -> "Web runtime failed: " + error);
            close(new IllegalStateException(error));
        }

        @JavascriptInterface
        public void resolve(int id, String json) {
            CompletableFuture<String> result = calls.get(id);
            if (result != null) result.complete(json);
        }

        @JavascriptInterface
        public void reject(int id, String error) {
            CompletableFuture<String> result = calls.get(id);
            if (result != null) result.completeExceptionally(new IllegalStateException(error));
        }
    }
}
