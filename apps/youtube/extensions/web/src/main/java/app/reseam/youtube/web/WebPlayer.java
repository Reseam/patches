// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.web;

import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.YouTubeContext;

/** Supplies a matching signature timestamp, URL solver and BotGuard minter for a player request. */
public final class WebPlayer {
    public static final String USER_AGENT = WebRuntime.USER_AGENT;
    private static final long PLAYER_REFRESH_MILLISECONDS = TimeUnit.MINUTES.toMillis(30);
    private static Generation current;

    /** Stream URL parameters for one player response: each challenge maps to its solution. */
    public record Unlocked(String poToken, Map<String, String> n, Map<String, String> sig) {}

    /**
     * A WEB profile and its matching token. Expiry uses {@link SystemClock#elapsedRealtime()}.
     * The session ID identifies the minter, so a late failure cannot invalidate its replacement.
     * The token is empty only when the page's player configuration does not request attestation.
     */
    public record Attestation(JSONObject client, String poToken, long expiresAt, String sessionId) {}

    private WebPlayer() {}

    /** Prepares the runtime asynchronously; requests can also start it without a warm-up. */
    public static synchronized void warmUp() {
        generation();
    }

    /**
     * Pins the player script until the returned session is closed. The deadline uses
     * {@link SystemClock#elapsedRealtime()}; opening and unlocking must run off the main thread.
     * A caller's deadline does not cancel shared preparation. Fatal runtime or preparation
     * failures retire the generation so a subsequent request can recover without restarting.
     */
    public static Session open(long deadline) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("The web player cannot be awaited on the main thread");
        }
        Session session;
        synchronized (WebPlayer.class) {
            Generation generation = generation();
            generation.users++;
            session = new Session(generation, deadline);
        }
        try {
            session.signatureTimestamp = session.await(session.generation.prepared);
            return session;
        } catch (Exception exception) {
            session.close();
            throw exception;
        }
    }

    private static Generation generation() {
        if (current == null || current.runtime.isClosed()
                || SystemClock.elapsedRealtime() - current.createdAt >= PLAYER_REFRESH_MILLISECONDS) {
            if (current != null) {
                current.retired = true;
                current.closeIfUnused();
            }
            current = new Generation();
        }
        return current;
    }

    private static final class Generation {
        final long createdAt = SystemClock.elapsedRealtime();
        final WebRuntime runtime = new WebRuntime(YouTubeContext.get());
        final CompletableFuture<Integer> prepared = runtime.call("web.prepare", new JSONArray().put(!TokenServer.selected()))
                .thenApply(Integer::valueOf);
        int users;
        boolean retired;

        Generation() {
            prepared.whenComplete((timestamp, error) -> {
                if (error != null) {
                    runtime.close(error);
                    Logger.error(() -> "Web player preparation failed", error);
                } else Logger.debug(() -> "Web player prepared, signature timestamp " + timestamp);
            });
        }

        void closeIfUnused() {
            if (retired && users == 0) runtime.close(new IllegalStateException("Web player refreshed"));
        }
    }

    /** One player request's lease on a runtime. Close after resolving its URLs, including on failure. */
    public static final class Session implements AutoCloseable {
        private final Generation generation;
        private final long deadline;
        private int signatureTimestamp;
        private boolean closed;

        private Session(Generation generation, long deadline) {
            this.generation = generation;
            this.deadline = deadline;
        }

        /** The timestamp of the exact script this session will use to solve stream challenges. */
        public int signatureTimestamp() {
            return signatureTimestamp;
        }

        /**
         * Mints the page's configured token for the requested video without borrowing Android
         * visitor data. Uses this session's deadline and fails on a closed session, a main-thread
         * call, timeout, invalid page configuration or attestation failure.
         */
        public Attestation attest(String videoId) throws Exception {
            if (closed) throw new IllegalStateException("Web player session is closed");
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("The web player cannot be awaited on the main thread");
            }
            if (TokenServer.selected()) return attestOnServer(videoId);
            CompletableFuture<String> call = generation.runtime.call("web.attest", new JSONArray().put(videoId));
            try {
                JSONObject result = new JSONObject(await(call));
                return new Attestation(result.getJSONObject("client"), result.getString("poToken"),
                        result.getLong("expiresAt"), result.getString("sessionId"));
            } finally {
                call.cancel(false);
            }
        }

        private Attestation attestOnServer(String videoId) throws Exception {
            CompletableFuture<String> call = generation.runtime.call("web.profile", new JSONArray());
            JSONObject profile;
            try {
                profile = new JSONObject(await(call));
            } finally {
                call.cancel(false);
            }
            JSONObject client = new JSONObject(profile.getJSONObject("client").toString());
            String sessionId = UUID.randomUUID().toString();
            switch (profile.getString("binding")) {
                case "none" -> {
                    return new Attestation(client, "", Long.MAX_VALUE, sessionId);
                }
                case "content" -> {
                    TokenServer.Token token = TokenServer.mint(videoId, deadline);
                    return new Attestation(client, token.poToken(), token.expiresAt(), sessionId);
                }
                default -> throw new IllegalStateException("The page requests a session-bound PoToken, which the token server cannot mint");
            }
        }

        /**
         * Retires the minter only if its ID still matches; a newer minter is left intact.
         * Used before renewing expired or challenged attestation. Uses this session's deadline.
         */
        public void invalidateAttestation(String sessionId) throws Exception {
            if (closed) throw new IllegalStateException("Web player session is closed");
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("The web player cannot be awaited on the main thread");
            }
            CompletableFuture<String> call = generation.runtime.call("web.invalidateAttestation", new JSONArray().put(sessionId));
            try {
                await(call);
            } finally {
                call.cancel(false);
            }
        }

        /** The WEB player response, requested by the page with the browser's network stack and origin. */
        public byte[] player(String body, JSONObject client) throws Exception {
            if (closed) throw new IllegalStateException("Web player session is closed");
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("The web player cannot be awaited on the main thread");
            }
            CompletableFuture<String> call = generation.runtime.call("web.player", new JSONArray().put(body).put(client));
            try {
                return Base64.decode(new JSONObject(await(call)).getString("response"), Base64.DEFAULT);
            } finally {
                call.cancel(false);
            }
        }

        /** Solves only URL challenges, without minting an unrelated token or changing attestation. */
        public Map<String, String> solveN(Collection<String> challenges) throws Exception {
            if (closed) throw new IllegalStateException("Web player session is closed");
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("The web player cannot be awaited on the main thread");
            }
            JSONObject input = new JSONObject().put("n", new JSONArray(challenges));
            CompletableFuture<String> call = generation.runtime.call("web.solve", new JSONArray().put(input));
            try {
                JSONObject result = new JSONObject(await(call));
                return solutions(result.getJSONObject("n"), challenges);
            } finally {
                call.cancel(false);
            }
        }

        /** Mints a token bound to the request's visitor data and solves that response's challenges. */
        public Unlocked unlock(String binding, Collection<String> n, Collection<String> sig) throws Exception {
            if (closed) throw new IllegalStateException("Web player session is closed");
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("The web player cannot be awaited on the main thread");
            }
            CompletableFuture<String> call = null;
            try {
                JSONObject challenges = new JSONObject().put("n", new JSONArray(n)).put("sig", new JSONArray(sig));
                call = generation.runtime.call("web.unlock", new JSONArray().put(binding).put(challenges));
                JSONObject result = new JSONObject(await(call));
                String token = result.getString("poToken");
                if (token.isEmpty()) throw new IllegalStateException("Web player returned an empty PoToken");
                return new Unlocked(token, solutions(result.getJSONObject("n"), n), solutions(result.getJSONObject("sig"), sig));
            } finally {
                // Detach this request on timeout/interruption without cancelling shared preparation.
                // A late bridge reply to a cancelled call is ignored by the runtime.
                if (call != null) call.cancel(false);
            }
        }

        private <T> T await(CompletableFuture<T> future) throws Exception {
            if (closed) throw new IllegalStateException("Web player session is closed");
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("The web player cannot be awaited on the main thread");
            }
            try {
                long remaining = deadline - SystemClock.elapsedRealtime();
                if (remaining <= 0) throw new TimeoutException("Web player request deadline exceeded");
                return future.get(remaining, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw exception;
            }
        }

        @Override
        public void close() {
            synchronized (WebPlayer.class) {
                if (closed) return;
                closed = true;
                generation.users--;
                generation.closeIfUnused();
            }
        }
    }

    private static Map<String, String> solutions(JSONObject solved, Collection<String> expected) throws JSONException {
        Map<String, String> solutions = new HashMap<>();
        for (Iterator<String> keys = solved.keys(); keys.hasNext(); ) {
            String challenge = keys.next();
            solutions.put(challenge, solved.getString(challenge));
        }
        for (String challenge : expected) {
            String solution = solutions.get(challenge);
            if (solution == null || solution.isEmpty()) throw new JSONException("Missing player challenge solution");
        }
        return solutions;
    }
}
