// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.web;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import app.reseam.youtube.core.Logger;

/**
 * A hidden WebView at the www.youtube.com origin running the scripts the patch bundles under
 * {@code assets/reseam/web}. The origin lets them call YouTube's endpoints as the web player does,
 * and the WebView is a real browser, which BotGuard requires. All WebView calls stay on the main
 * thread; results come back through futures.
 */
final class WebRuntime {
    private static final String HOST = "www.youtube.com";
    private static final String ROOT = "/reseam/web/";
    private static final String ASSETS = "reseam/web/";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final CompletableFuture<Void> ready = new CompletableFuture<>();
    private final Map<Integer, CompletableFuture<String>> calls = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger();
    private WebView view;

    /** Must run on the main thread. */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    void start(Context context) {
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
                    Logger.error(() -> "Missing web asset " + path, exception);
                    return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null, null);
                }
            }
        });
        view.loadUrl("https://" + HOST + ROOT + "index.html");
    }

    /** Calls the async JavaScript function `function` with `arguments`; completes with its result as JSON. */
    CompletableFuture<String> call(String function, JSONArray arguments) {
        int id = nextId.incrementAndGet();
        CompletableFuture<String> result = new CompletableFuture<>();
        calls.put(id, result);
        String script = "host.call(" + id + ", " + function + ", " + arguments + ")";
        ready.whenComplete((unused, error) -> {
            if (error != null) complete(id, null, error.getMessage());
            else main.post(() -> view.evaluateJavascript(script, null));
        });
        return result;
    }

    private void complete(int id, String value, String error) {
        CompletableFuture<String> result = calls.remove(id);
        if (result == null) return;
        if (error == null) result.complete(value);
        else result.completeExceptionally(new IllegalStateException(error));
    }

    private final class Bridge {
        @JavascriptInterface
        public void ready() {
            ready.complete(null);
        }

        @JavascriptInterface
        public void fail(String error) {
            Logger.error(() -> "Web runtime failed to start: " + error);
            ready.completeExceptionally(new IllegalStateException(error));
        }

        @JavascriptInterface
        public void resolve(int id, String json) {
            complete(id, json, null);
        }

        @JavascriptInterface
        public void reject(int id, String error) {
            complete(id, null, error);
        }
    }
}
