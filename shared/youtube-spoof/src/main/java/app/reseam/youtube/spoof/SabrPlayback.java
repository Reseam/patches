// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.os.SystemClock;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.web.WebPlayer;

/**
 * Bridges validated SABR responses into YouTube's native SABR player. Ownership follows the exact
 * installed protobuf and native transport/token-manager instances, including concurrent prefetch.
 * Weak identity associations release abandoned native owners; values never retain their map keys.
 */
public final class SabrPlayback {
    private static final int PROTECTION_OK = 1;
    private static final int ATTESTATION_PENDING = 2;
    private static final int ATTESTATION_REQUIRED = 3;
    private static final long REFRESH_TIMEOUT_MILLISECONDS = 30_000;
    private static final long REFRESH_MARGIN_MILLISECONDS = 30_000;
    private static final WeakIdentityMap<SabrData> STREAMS = new WeakIdentityMap<>();
    private static final WeakIdentityMap<Session> TRANSPORTS = new WeakIdentityMap<>();
    private static final WeakIdentityMap<Session> TOKENS = new WeakIdentityMap<>();
    private static final WeakIdentityMap<SabrResponse> RESPONSES = new WeakIdentityMap<>();
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(50));

    private SabrPlayback() {}

    /** Called only after successfully installing the matching native streaming-data message. */
    public static void installed(Object nativeStreams, StreamingData streams) {
        if (streams.sabr != null) STREAMS.put(nativeStreams, streams.sabr);
        else STREAMS.remove(nativeStreams);
    }

    /**
     * Builds the configuration before attaching transport state. Unowned Android responses keep
     * their native transport and token manager, including when native owners are reused.
     */
    public static Object prepare(Object streams, Object config, Object transport, Object tokens) {
        SabrData data = STREAMS.get(streams);
        if (data == null) {
            TRANSPORTS.remove(transport);
            TOKENS.remove(tokens);
            return config;
        }
        try {
            Object replacement = replaceConfig(config, data.nativeCommonConfig());
            Session session = new Session(data);
            TRANSPORTS.put(transport, session);
            TOKENS.put(tokens, session);
            Logger.debug(() -> "Native " + data.client() + " SABR playback created for " + data.videoId());
            return replacement;
        } catch (Exception exception) {
            TRANSPORTS.remove(transport);
            TOKENS.remove(tokens);
            Logger.error(() -> "Could not prepare native SABR playback", exception);
            return config;
        }
    }

    /**
     * Token for an owned transport or token manager. Returns the original bytes for an unowned
     * instance or an invalid token encoding; a null original remains null on those paths.
     */
    public static byte[] token(Object owner, byte[] original) {
        Session session = TOKENS.get(owner);
        if (session == null) session = TRANSPORTS.get(owner);
        try {
            return session == null ? original : decodeToken(session.attestation);
        } catch (IllegalArgumentException exception) {
            Logger.error(() -> "Invalid SABR token encoding", exception);
            return original;
        }
    }

    /**
     * Handles a native refresh asynchronously, sharing pending work for this playback. Returns
     * false for an unowned Android manager. An owned callback receives an empty token on failure;
     * the native player owns retry limits and timing. A null callback still refreshes cached state.
     */
    public static boolean refresh(Object manager, Object callback) {
        Session session = TOKENS.get(manager);
        if (session == null) return false;
        session.refresh(session.rejectedSession).whenComplete((attestation, error) -> {
            if (error != null) Logger.error(() -> "SABR token refresh failed", error);
            if (callback != null) {
                try {
                    tokenMinted(callback, error == null ? decodeToken(attestation) : new byte[0]);
                } catch (Exception exception) {
                    Logger.error(() -> "SABR token callback failed", exception);
                }
            }
        });
        return true;
    }

    /** Rewrites only an owned native SABR request; native ABR, cookies and timing remain intact. */
    public static byte[] request(Object transport, Object callback, byte[] original) {
        Session session = TRANSPORTS.get(transport);
        if (session == null || original == null) return original;
        try {
            // VideoPlaybackAbrRequest.streamerContext (19): clientInfo (1), poToken (2).
            byte[] context = Proto.bytes(original, 19);
            if (context == null) return original;
            WebPlayer.Attestation attestation = session.attestation;
            if (attestation.expiresAt() - SystemClock.elapsedRealtime() < REFRESH_MARGIN_MILLISECONDS) {
                session.refresh(attestation.sessionId());
            }
            byte[] rewritten = Proto.replace(context, Map.of(1, clientInfo(session.data.client(), attestation.client()), 2, decodeToken(attestation)));
            byte[] body = Proto.replace(original, Map.of(19, session.contexts.append(rewritten)));
            RESPONSES.put(callback, new SabrResponse((type, message) -> session.control(type, message, attestation.sessionId())));
            return body;
        } catch (Exception exception) {
            Logger.error(() -> "Could not adapt SABR request", exception);
            return original;
        }
    }

    /** Observes control messages before native consumption without copying media payloads. */
    public static void response(Object callback, ByteBuffer buffer) {
        SabrResponse response = RESPONSES.get(callback);
        if (response == null || buffer == null) return;
        try {
            response.read(buffer);
        } catch (Exception exception) {
            RESPONSES.remove(callback);
            Logger.error(() -> "Invalid SABR control response", exception);
        }
    }

    /** Releases a completed or failed request's incremental decoder. */
    public static void completed(Object callback) {
        RESPONSES.remove(callback);
    }

    private static byte[] decodeToken(WebPlayer.Attestation attestation) {
        return Base64.decode(attestation.poToken(), Base64.URL_SAFE);
    }

    private static byte[] clientInfo(ClientType type, JSONObject client) throws Exception {
        // ClientInfo: clientName (16), clientVersion (17), hl (21), gl (22).
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Proto.writeNumber(output, 16, type.clientId);
        Proto.writeBytes(output, 17, client.getString("clientVersion").getBytes(StandardCharsets.UTF_8));
        Proto.writeBytes(output, 21, client.optString("hl", "en").getBytes(StandardCharsets.UTF_8));
        Proto.writeBytes(output, 22, client.optString("gl", "US").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private static final class Session {
        final SabrData data;
        final SabrContexts contexts = new SabrContexts();
        volatile WebPlayer.Attestation attestation;
        volatile String rejectedSession;
        private CompletableFuture<WebPlayer.Attestation> refreshing;

        Session(SabrData data) {
            this.data = data;
            attestation = data.attestation();
        }

        synchronized CompletableFuture<WebPlayer.Attestation> refresh(String expectedSessionId) {
            // Only WEB streams carry a PoToken; other clients have nothing to renew.
            if (data.client() != ClientType.WEB) return CompletableFuture.completedFuture(attestation);
            // A late rejection or expiry check must not retire an already renewed minter.
            String currentSessionId = attestation.sessionId();
            if (expectedSessionId != null && !expectedSessionId.equals(currentSessionId)) {
                return CompletableFuture.completedFuture(attestation);
            }
            if (refreshing != null) return refreshing;
            CompletableFuture<WebPlayer.Attestation> result = new CompletableFuture<>();
            refreshing = result;
            try {
                EXECUTOR.execute(() -> {
                    try (WebPlayer.Session web = WebPlayer.open(SystemClock.elapsedRealtime() + REFRESH_TIMEOUT_MILLISECONDS)) {
                        web.invalidateAttestation(currentSessionId);
                        WebPlayer.Attestation replacement = web.attest(data.videoId());
                        synchronized (this) {
                            attestation = replacement;
                        }
                        Logger.debug(() -> "SABR token refreshed for " + data.videoId());
                        result.complete(replacement);
                    } catch (Exception exception) {
                        result.completeExceptionally(exception);
                    } finally {
                        synchronized (this) {
                            if (refreshing == result) refreshing = null;
                        }
                    }
                });
            } catch (RuntimeException exception) {
                refreshing = null;
                result.completeExceptionally(exception);
            }
            return result;
        }

        void control(int type, byte[] message, String sessionId) {
            switch (type) {
                case SabrResponse.RELOAD_PLAYER_RESPONSE ->
                        Logger.debug(() -> "SABR requested a player response reload for " + data.videoId());
                case SabrResponse.CONTEXT_UPDATE -> contexts.update(message);
                case SabrResponse.CONTEXT_SENDING_POLICY -> contexts.policy(message);
                case SabrResponse.STREAM_PROTECTION_STATUS -> {
                    long status = Proto.number(message, 1, 0);
                    synchronized (this) {
                        if ((status == ATTESTATION_PENDING || status == ATTESTATION_REQUIRED)
                                && attestation.sessionId().equals(sessionId)) {
                            rejectedSession = sessionId;
                        } else if (status == PROTECTION_OK && sessionId.equals(rejectedSession)) {
                            rejectedSession = null;
                        }
                    }
                    Logger.debug(() -> "SABR protection status " + status + " for " + data.videoId());
                }
                default -> { }
            }
        }
    }

    /** Implemented at patch time against the app's retained configuration and protobuf types. */
    private static Object replaceConfig(Object original, Object common) {
        throw new UnsupportedOperationException("Implemented by the SABR patch");
    }

    /** Parses with the native extension registry before publishing a replacement response. */
    static Object parseCommonConfig(byte[] common) {
        throw new UnsupportedOperationException("Implemented by the SABR patch");
    }

    /** Implemented at patch time against the app's native PoToken callback interface. */
    private static void tokenMinted(Object callback, byte[] token) {
        throw new UnsupportedOperationException("Implemented by the SABR patch");
    }
}
