// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.web;

import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.YouTubeContext;

/**
 * What a web client's streams need: the web player's signature timestamp for the player request,
 * then a PoToken and the solved `n` and `sig` challenges for the stream URLs it returns.
 */
public final class WebPlayer {
    private static final long TIMEOUT_SECONDS = 20;
    private static final WebRuntime RUNTIME = new WebRuntime();

    private static CompletableFuture<Void> started;
    private static CompletableFuture<Integer> prepared;

    /** Stream URL parameters for one player response: each challenge maps to its solution. */
    public record Unlocked(String poToken, Map<String, String> n, Map<String, String> sig) {}

    private WebPlayer() {}

    /** Starts the runtime once the main thread is idle, so it is ready before the first video. */
    public static synchronized void warmUp() {
        if (started != null) return;
        started = new CompletableFuture<>();
        Looper.getMainLooper().getQueue().addIdleHandler(() -> {
            RUNTIME.start(YouTubeContext.get());
            started.complete(null);
            prepare();
            return false;
        });
    }

    /** Blocks until the runtime is prepared; must not run on the main thread. */
    public static int signatureTimestamp() throws Exception {
        return await(prepare());
    }

    /** A PoToken bound to `binding`, a visitor data string, and the solution of each challenge. */
    public static Unlocked unlock(String binding, Collection<String> n, Collection<String> sig) throws Exception {
        signatureTimestamp();
        JSONObject challenges = new JSONObject().put("n", new JSONArray(n)).put("sig", new JSONArray(sig));
        JSONObject result = new JSONObject(await(RUNTIME.call("web.unlock",
                new JSONArray().put(binding).put(challenges))));
        return new Unlocked(result.getString("poToken"), solutions(result.getJSONObject("n")),
                solutions(result.getJSONObject("sig")));
    }

    private static Map<String, String> solutions(JSONObject solved) throws JSONException {
        Map<String, String> solutions = new HashMap<>();
        for (Iterator<String> keys = solved.keys(); keys.hasNext(); ) {
            String challenge = keys.next();
            solutions.put(challenge, solved.getString(challenge));
        }
        return solutions;
    }

    /** The current preparation, restarted when the last one failed (for example while offline). */
    private static synchronized CompletableFuture<Integer> prepare() {
        if (started == null) throw new IllegalStateException("WebPlayer.warmUp has not run");
        if (prepared == null || prepared.isCompletedExceptionally()) {
            long start = System.currentTimeMillis();
            prepared = started
                    .thenCompose(unused -> RUNTIME.call("web.prepare", new JSONArray()))
                    .thenApply(Integer::valueOf);
            prepared.whenComplete((timestamp, error) -> {
                if (error != null) Logger.error(() -> "Web player preparation failed", error);
                else Logger.debug(() -> "Web player prepared in " + (System.currentTimeMillis() - start)
                        + " ms, signature timestamp " + timestamp);
            });
        }
        return prepared;
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("The web player cannot be awaited on the main thread");
        }
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
