// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.dislike.requests;

import static app.reseam.youtube.dislike.RuntimeUtils.str;
import static app.reseam.youtube.dislike.requests.ReturnYouTubeDislikeRoutes.getRYDConnectionFromRoute;

import android.util.Base64;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.dislike.RuntimeUtils;
import app.reseam.youtube.dislike.requests.Requester;
import app.reseam.youtube.dislike.ReturnYouTubeDislike;
import app.reseam.youtube.core.Settings;

public class ReturnYouTubeDislikeAPI {
    /**
     * {@link #fetchVotes(String)} TCP connection timeout.
     */
    private static final int API_GET_VOTES_TCP_TIMEOUT_MILLISECONDS = 3 * 1000; // 3 Seconds.

    /**
     * {@link #fetchVotes(String)} HTTP read timeout.
     * To locally debug and force timeouts, change this to a very small number (ie: 100)
     */
    private static final int API_GET_VOTES_HTTP_TIMEOUT_MILLISECONDS = 7 * 1000; // 7 Seconds.

    /**
     * Default connection and response timeout for voting and registration.
     *
     * Voting and user registration runs in the background and has no urgency
     * so this can be a larger value.
     */
    private static final int API_REGISTER_VOTE_TIMEOUT_MILLISECONDS = 60 * 1000; // 60 Seconds.

    /**
     * Response code of a successful API call
     */
    private static final int HTTP_STATUS_CODE_SUCCESS = 200;

    /**
     * RYD API sometimes returns 401 (authorization error), even though the user id is valid.
     * There is no known fix for this (resetting to a different user id does not fix it),
     * so instead just quietly ignore the error.
     *
     * See <a href="https://github.com/Anarios/return-youtube-dislike/issues/1153">RYD bug report</a>.
     */
    private static final int HTTP_STATUS_CODE_UNAUTHORIZED = 401;

    /**
     * Indicates a client rate limit has been reached and the client must back off.
     */
    private static final int HTTP_STATUS_CODE_RATE_LIMIT = 429;

    /**
     * How long to wait until API calls are resumed, if the API requested a back off.
     * No clear guideline of how long to wait until resuming.
     */
    private static final int BACKOFF_RATE_LIMIT_MILLISECONDS = 10 * 60 * 1000; // 10 Minutes.

    /**
     * How long to wait until API calls are resumed, if any connection error occurs.
     */
    private static final int BACKOFF_CONNECTION_ERROR_MILLISECONDS = 2 * 60 * 1000; // 2 Minutes.

    /**
     * If non zero, then the system time of when API calls can resume.
     */
    private static volatile long timeToResumeAPICalls;

    /**
     * If the last API getVotes call failed for any reason (including server requested rate limit).
     * Used to prevent showing repeat connection toasts when the API is down.
     */
    private static volatile boolean lastApiCallFailed;

    /**
     * Number of times {@link #HTTP_STATUS_CODE_RATE_LIMIT} was requested by RYD api.
     * Does not include network calls attempted while rate limit is in effect,
     * and does not include rate limit imposed if a fetch fails.
     */
    private static volatile int numberOfRateLimitRequestsEncountered;

    /**
     * Number of network calls made in {@link #fetchVotes(String)}
     */
    private static volatile int fetchCallCount;

    /**
     * Number of times {@link #fetchVotes(String)} failed due to timeout or any other error.
     * This does not include when rate limit requests are encountered.
     */
    private static volatile int fetchCallNumberOfFailures;

    /**
     * Total time spent waiting for {@link #fetchVotes(String)} network call to complete.
     * Value does not persist on app shut down.
     */
    private static volatile long fetchCallResponseTimeTotal;

    /**
     * Round trip network time for the most recent call to {@link #fetchVotes(String)}
     */
    private static volatile long fetchCallResponseTimeLast;
    private static volatile long fetchCallResponseTimeMin;
    private static volatile long fetchCallResponseTimeMax;

    public static final int FETCH_CALL_RESPONSE_TIME_VALUE_RATE_LIMIT = -1;

    /**
     * If rate limit was hit, this returns {@link #FETCH_CALL_RESPONSE_TIME_VALUE_RATE_LIMIT}
     */
    public static long getFetchCallResponseTimeLast() {
        return fetchCallResponseTimeLast;
    }
    public static long getFetchCallResponseTimeMin() {
        return fetchCallResponseTimeMin;
    }
    public static long getFetchCallResponseTimeMax() {
        return fetchCallResponseTimeMax;
    }
    public static long getFetchCallResponseTimeAverage() {
        return fetchCallCount == 0 ? 0 : (fetchCallResponseTimeTotal / fetchCallCount);
    }
    public static int getFetchCallCount() {
        return fetchCallCount;
    }
    public static int getFetchCallNumberOfFailures() {
        return fetchCallNumberOfFailures;
    }
    public static int getNumberOfRateLimitRequestsEncountered() {
        return numberOfRateLimitRequestsEncountered;
    }

    private ReturnYouTubeDislikeAPI() {
    } // utility class

    /**
     * Clears any backoff rate limits in effect.
     * Should be called if RYD is turned on/off.
     */
    public static void resetRateLimits() {
        if (lastApiCallFailed || timeToResumeAPICalls != 0) {
            Logger.debug(() -> "Reset rate limit");
        }
        lastApiCallFailed = false;
        timeToResumeAPICalls = 0;
    }

    /**
     * @return True, if api rate limit is in effect.
     */
    private static boolean checkIfRateLimitInEffect(String apiEndPointName) {
        if (timeToResumeAPICalls == 0) {
            return false;
        }
        final long now = System.currentTimeMillis();
        if (now > timeToResumeAPICalls) {
            timeToResumeAPICalls = 0;
            return false;
        }
        Logger.debug(() -> "Ignoring api call " + apiEndPointName + " as rate limit is in effect");
        return true;
    }

    /**
     * @return True, if a client rate limit was requested
     */
    private static boolean checkIfRateLimitWasHit(int httpResponseCode) {
        return httpResponseCode == HTTP_STATUS_CODE_RATE_LIMIT;
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField") // Don't care, fields are only estimates.
    private static void updateRateLimitAndStats(long timeNetworkCallStarted, boolean connectionError, boolean rateLimitHit) {
        if (connectionError && rateLimitHit) {
            throw new IllegalArgumentException();
        }
        final long responseTimeOfFetchCall = System.currentTimeMillis() - timeNetworkCallStarted;
        fetchCallResponseTimeTotal += responseTimeOfFetchCall;
        fetchCallResponseTimeMin = (fetchCallResponseTimeMin == 0) ? responseTimeOfFetchCall : Math.min(responseTimeOfFetchCall, fetchCallResponseTimeMin);
        fetchCallResponseTimeMax = Math.max(responseTimeOfFetchCall, fetchCallResponseTimeMax);
        fetchCallCount++;
        if (connectionError) {
            timeToResumeAPICalls = System.currentTimeMillis() + BACKOFF_CONNECTION_ERROR_MILLISECONDS;
            fetchCallResponseTimeLast = responseTimeOfFetchCall;
            fetchCallNumberOfFailures++;
            lastApiCallFailed = true;
        } else if (rateLimitHit) {
            Logger.debug(() -> "API rate limit was hit. Stopping API calls for the next "
                    + BACKOFF_RATE_LIMIT_MILLISECONDS + " seconds");
            timeToResumeAPICalls = System.currentTimeMillis() + BACKOFF_RATE_LIMIT_MILLISECONDS;
            numberOfRateLimitRequestsEncountered++;
            fetchCallResponseTimeLast = FETCH_CALL_RESPONSE_TIME_VALUE_RATE_LIMIT;
            if (!lastApiCallFailed && Settings.getBoolean("ryd_toast_on_connection_error", true)) {
                RuntimeUtils.showToastLong(str("revanced_ryd_failure_client_rate_limit_requested"));
            }
            lastApiCallFailed = true;
        } else {
            fetchCallResponseTimeLast = responseTimeOfFetchCall;
            lastApiCallFailed = false;
        }
    }

    /**
     * @param toastDuration Either {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}.
     */
    public static void handleConnectionError(String toastMessage,
                                              Integer responseCode,
                                              Exception ex,
                                              Integer toastDuration) {
        if (!lastApiCallFailed && Settings.getBoolean("ryd_toast_on_connection_error", true)) {
            if (responseCode != null && responseCode == HTTP_STATUS_CODE_UNAUTHORIZED) {
                Logger.info(() -> "Ignoring status code " + HTTP_STATUS_CODE_UNAUTHORIZED
                        + " (API authorization error)");
                return; // Do not set api failure field.
            } else if (toastDuration != null) {
                RuntimeUtils.showToast(toastMessage, toastDuration);
            }
        }
        lastApiCallFailed = true;

        Logger.info(() -> toastMessage, ex);
    }

    /**
     * @return NULL if fetch failed, or if a rate limit is in effect.
     */

    public static RYDVoteData fetchVotes(String videoId) {
        RuntimeUtils.verifyOffMainThread();
        Objects.requireNonNull(videoId);

        if (checkIfRateLimitInEffect("fetchVotes")) {
            return null;
        }
        Logger.debug(() -> "Fetching votes for: " + videoId);
        final long timeNetworkCallStarted = System.currentTimeMillis();

        HttpURLConnection connection = null;
        try {
            connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.GET_DISLIKES, videoId);
            // request headers, as per https://returnyoutubedislike.com/docs/fetching
            // the documentation says to use 'Accept:text/html', but the RYD browser plugin uses 'Accept:application/json'
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Connection", "keep-alive"); // keep-alive is on by default with http 1.1, but specify anyway
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setUseCaches(false);
            connection.setConnectTimeout(API_GET_VOTES_TCP_TIMEOUT_MILLISECONDS); // timeout for TCP connection to server
            connection.setReadTimeout(API_GET_VOTES_HTTP_TIMEOUT_MILLISECONDS); // timeout for server response


            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                updateRateLimitAndStats(timeNetworkCallStarted, false, true);
                return null;
            }

            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                JSONObject json = Requester.parseJSONObject(connection);
                try {
                    RYDVoteData votingData = new RYDVoteData(json);
                    updateRateLimitAndStats(timeNetworkCallStarted, false, false);
                    Logger.debug(() -> "Voting data fetched: " + votingData);
                    return votingData;
                } catch (JSONException ex) {
                    Logger.error(() -> "Failed to parse video: " + videoId + " JSON: " + json, ex);
                    // fall thru to update statistics
                }
            } else {
                // Unexpected response code.  Most likely RYD is temporarily broken.
                handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode),
                        responseCode, null, Toast.LENGTH_LONG);
            }
        } catch (SocketTimeoutException ex) {
            handleConnectionError((str("revanced_ryd_failure_connection_timeout")), null, ex, Toast.LENGTH_SHORT);
        } catch (IOException ex) {
            handleConnectionError((str("revanced_ryd_failure_generic", ex.getMessage())), null, ex, Toast.LENGTH_LONG);
        } catch (Exception ex) {
            // should never happen
            Logger.error(() -> "fetchVotes failure", ex);
        } finally {
            if (connection != null) connection.disconnect();
        }

        updateRateLimitAndStats(timeNetworkCallStarted, true, false);
        return null;
    }

    /**
     * @return The newly created and registered user ID.  Returns NULL if registration failed.
     */

    public static String registerAsNewUser() {
        RuntimeUtils.verifyOffMainThread();
        HttpURLConnection connection = null;
        try {
            if (checkIfRateLimitInEffect("registerAsNewUser")) {
                return null;
            }
            String userID = randomString(36);
            Logger.debug(() -> "Trying to register new user");

            connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.GET_REGISTRATION, userID);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(API_REGISTER_VOTE_TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(API_REGISTER_VOTE_TIMEOUT_MILLISECONDS);

            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                return null;
            }
            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                JSONObject json = Requester.parseJSONObject(connection);
                String challenge = json.getString("challenge");
                int difficulty = json.getInt("difficulty");

                String solution = solvePuzzle(challenge, difficulty);
                return confirmRegistration(userID, solution);
            }

            handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode),
                    responseCode, null, Toast.LENGTH_LONG);
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_ryd_failure_connection_timeout"), null, ex, Toast.LENGTH_SHORT);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_ryd_failure_generic", "registration failed"), null, ex, Toast.LENGTH_LONG);
        } catch (Exception ex) {
            Logger.error(() -> "Failed to register user", ex); // should never happen
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }


    private static String confirmRegistration(String userID, String solution) {
        RuntimeUtils.verifyOffMainThread();
        Objects.requireNonNull(userID);
        Objects.requireNonNull(solution);
        HttpURLConnection connection = null;
        try {
            if (checkIfRateLimitInEffect("confirmRegistration")) {
                return null;
            }
            Logger.debug(() -> "Trying to confirm registration with solution: " + solution);

            connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.CONFIRM_REGISTRATION, userID);
            applyCommonPostRequestSettings(connection);

            String jsonInputString = "{\"solution\": \"" + solution + "\"}";
            byte[] body = jsonInputString.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body);
            }

            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                return null;
            }
            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                Logger.debug(() -> "Registration confirmation successful");
                return userID;
            }

            Logger.info(() -> "Failed to confirm registration, HTTP " + responseCode);
            handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode),
                    responseCode, null, Toast.LENGTH_LONG);
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_ryd_failure_connection_timeout"), null, ex, Toast.LENGTH_SHORT);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_ryd_failure_generic", "confirm registration failed"),
                    null, ex, Toast.LENGTH_LONG);
        } catch (Exception ex) {
            Logger.error(() -> "Failed to confirm registration", ex);
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    /**
     * Must call off main thread, as this will make a network call if user is not yet registered.
     *
     * @return ReturnYouTubeDislike user ID. If user registration has never happened
     * and the network call fails, this returns NULL.
     */

    private static String getUserID() {
        RuntimeUtils.verifyOffMainThread();

        String userID = app.reseam.runtime.settings.ReseamSettings.getString("you_tube_settings.ryd_user_id", "");
        if (!userID.isEmpty()) {
            return userID;
        }

        userID = registerAsNewUser();
        if (userID != null) {
            app.reseam.runtime.settings.ReseamSettings.setString("you_tube_settings.ryd_user_id", userID);
        }
        return userID;
    }

    public static boolean sendVote(String videoId, ReturnYouTubeDislike.Vote vote) {
        RuntimeUtils.verifyOffMainThread();
        Objects.requireNonNull(videoId);
        Objects.requireNonNull(vote);

        HttpURLConnection connection = null;
        try {
            String userID = getUserID();
            if (userID == null) return false;

            if (checkIfRateLimitInEffect("sendVote")) {
                return false;
            }
            Logger.debug(() -> "Trying to vote for video: " + videoId + " with vote: " + vote);

            connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.SEND_VOTE);
            applyCommonPostRequestSettings(connection);

            String voteJsonString = "{\"userId\": \"" + userID + "\", \"videoId\": \"" + videoId + "\", \"value\": \"" + vote.value + "\"}";
            byte[] body = voteJsonString.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body);
            }

            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                return false;
            }
            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                JSONObject json = Requester.parseJSONObject(connection);
                String challenge = json.getString("challenge");
                int difficulty = json.getInt("difficulty");

                String solution = solvePuzzle(challenge, difficulty);
                return confirmVote(videoId, userID, solution);
            }

            Logger.info(() -> "Failed to send vote for video: " + videoId + " vote: " + vote
                    + " response code was: " + responseCode);
            handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode),
                    responseCode, null, Toast.LENGTH_LONG);
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_ryd_failure_connection_timeout"), null, ex, Toast.LENGTH_SHORT);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_ryd_failure_generic", "send vote failed"), null, ex, Toast.LENGTH_LONG);
        } catch (Exception ex) {
            // should never happen
            Logger.error(() -> "Failed to send vote for video: " + videoId + " vote: " + vote, ex);
        } finally {
            if (connection != null) connection.disconnect();
        }
        return false;
    }

    private static boolean confirmVote(String videoId, String userID, String solution) {
        RuntimeUtils.verifyOffMainThread();
        Objects.requireNonNull(videoId);
        Objects.requireNonNull(userID);
        Objects.requireNonNull(solution);

        HttpURLConnection connection = null;
        try {
            if (checkIfRateLimitInEffect("confirmVote")) {
                return false;
            }
            Logger.debug(() -> "Trying to confirm vote for video: " + videoId + " solution: " + solution);
            connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.CONFIRM_VOTE);
            applyCommonPostRequestSettings(connection);

            String jsonInputString = "{\"userId\": \"" + userID + "\", \"videoId\": \"" + videoId + "\", \"solution\": \"" + solution + "\"}";
            byte[] body = jsonInputString.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body);
            }

            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                return false;
            }
            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                Logger.debug(() -> "Vote confirm successful for video: " + videoId);
                return true;
            }

            Logger.info(() -> "Failed to confirm vote for video: " + videoId
                    + " responseCode: " + responseCode);
            handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode),
                    responseCode, null, Toast.LENGTH_LONG);
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_ryd_failure_connection_timeout"), null, ex, Toast.LENGTH_SHORT);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_ryd_failure_generic", "confirm vote failed"),
                    null, ex, Toast.LENGTH_LONG);
        } catch (Exception ex) {
            Logger.error(() -> "Failed to confirm vote for video: " + videoId
                    + " solution: " + solution, ex); // should never happen
        } finally {
            if (connection != null) connection.disconnect();
        }
        return false;
    }

    private static void applyCommonPostRequestSettings(HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setConnectTimeout(API_REGISTER_VOTE_TIMEOUT_MILLISECONDS); // timeout for TCP connection to server
        connection.setReadTimeout(API_REGISTER_VOTE_TIMEOUT_MILLISECONDS); // timeout for server response
    }


    private static String solvePuzzle(String challenge, int difficulty) {
        final long timeSolveStarted = System.currentTimeMillis();
        byte[] decodedChallenge = Base64.decode(challenge, Base64.NO_WRAP);

        byte[] buffer = new byte[20];
        System.arraycopy(decodedChallenge, 0, buffer, 4, 16);

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex); // should never happen
        }

        final int maxCount = (int) (Math.pow(2, difficulty + 1) * 5);
        for (int i = 0; i < maxCount; i++) {
            buffer[0] = (byte) i;
            buffer[1] = (byte) (i >> 8);
            buffer[2] = (byte) (i >> 16);
            buffer[3] = (byte) (i >> 24);
            byte[] messageDigest = md.digest(buffer);

            if (countLeadingZeroes(messageDigest) >= difficulty) {
                String solution = Base64.encodeToString(new byte[]{buffer[0], buffer[1], buffer[2], buffer[3]}, Base64.NO_WRAP);
                Logger.debug(() -> "Found puzzle solution: " + solution + " of difficulty: " + difficulty
                        + " in: " + (System.currentTimeMillis() - timeSolveStarted) + " ms");
                return solution;
            }
        }

        // should never be reached
        throw new IllegalStateException("Failed to solve puzzle challenge: " + challenge + " difficulty: " + difficulty);
    }

    // https://stackoverflow.com/a/157202
    private static String randomString(int len) {
        String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rnd = new SecureRandom();

        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    private static int countLeadingZeroes(byte[] uInt8View) {
        int zeroes = 0;
        for (byte b : uInt8View) {
            int value = b & 0xFF;
            if (value == 0) {
                zeroes += 8;
            } else {
                int count = 1;
                if (value >>> 4 == 0) {
                    count += 4;
                    value <<= 4;
                }
                if (value >>> 6 == 0) {
                    count += 2;
                    value <<= 2;
                }
                zeroes += count - (value >>> 7);
                break;
            }
        }
        return zeroes;
    }
}
