// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later
package app.reseam.youtube.dislike;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;

import app.reseam.youtube.core.YouTubeContext;
import app.reseam.youtube.dislike.requests.RYDVoteData;
import app.reseam.youtube.dislike.requests.ReturnYouTubeDislikeAPI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Isolated Android runtime tests. Never loads the app's preferences or uses real HTTP. */
public final class RydDeviceRegression {
    private static final String REGULAR = "video_action_bar.eml|segmented_like_dislike_button.eml|TextType|";
    private static final String CHALLENGE = "{\"challenge\":\"AAAAAAAAAAAAAAAAAAAAAA==\",\"difficulty\":0}";
    private static final Map<String, Object> settings = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Response> responses = new ConcurrentLinkedQueue<>();
    private static final List<Response> opened = new ArrayList<>();
    private static final AtomicInteger unexpectedConnections = new AtomicInteger();
    private static int passed;

    public static void main(String[] args) {
        try {
            Locale.setDefault(Locale.US);
            Looper.prepareMainLooper();
            // app_process is not forked from the app zygote and has no preloaded font map.
            java.io.File fontConfig = new java.io.File("/system/etc/font_fallback.xml");
            if (fontConfig.exists() && !fontConfig.canRead()) {
                throw new IllegalStateException("Font config denies shell access; use RYD_TEST_ROOT=1 on a rooted test device");
            }
            android.graphics.Typeface.class.getDeclaredMethod("loadPreinstalledSystemFontMap").invoke(null);
            settings.put("you_tube_settings.ryd_toast_on_connection_error", false);
            settings.put("you_tube_settings.ryd_user_id", "isolated-test-user");
            YouTubeContext.init(testContext());
            URL.setURLStreamHandlerFactory(protocol -> new URLStreamHandler() {
                @Override protected HttpURLConnection openConnection(URL url) throws IOException {
                    Response response = responses.poll();
                    if (response == null) {
                        unexpectedConnections.incrementAndGet();
                        throw new IOException("Unscripted HTTP request blocked: " + url.getPath());
                    }
                    synchronized (opened) { opened.add(response); }
                    return response.connection(url);
                }
            });

            rendering();
            concurrency();
            nativeLabelState();
            requests();
            check(responses.isEmpty(), "All scripted responses consumed");
            check(unexpectedConnections.get() == 0, "No unscripted HTTP requests");
            System.out.println("PASS: " + passed + " RYD device regression checks; no real network or votes");
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void rendering() throws Exception {
        Response delayed = new Response(200, votes("pending", 1200, 123));
        delayed.release = new CountDownLatch(1);
        responses.add(delayed);
        ReturnYouTubeDislike data = ReturnYouTubeDislike.getFetchForVideoId("pending");
        check(delayed.entered.await(2, TimeUnit.SECONDS), "Fetch started in background");
        set(ReturnYouTubeDislikePatch.class, null, "currentVideoData", data);
        SpannableString original = new SpannableString("1.2K");
        original.setSpan(new ForegroundColorSpan(Color.WHITE), 0, original.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        long started = System.nanoTime();
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder(REGULAR), original) == original,
                "Pending render preserves original span");
        check(data.getDislikeCountText() == null, "Pending native label does not expose stale counts");
        check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 250, "Pending render never waits for HTTP");
        FutureTask<Void> vote = new FutureTask<>(() -> { data.setUserVote(ReturnYouTubeDislike.Vote.DISLIKE); return null; });
        new Thread(vote).start();
        vote.get(250, TimeUnit.MILLISECONDS);
        check(true, "Pending render does not hold vote monitor");
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(null, original) == original, "Null path is unchanged");
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder("unrelated"), original) == original,
                "Unrelated text is unchanged");
        delayed.release.countDown();
        awaitVotes(data);
        CharSequence replacement = ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder(REGULAR), original);
        check(replacement.toString().contains("124"), "Next bind uses fetched counts plus pending local vote");
        check(data.getDislikeCountText().equals("124"), "Native label uses completed fetch and local vote");
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder(REGULAR), replacement) == replacement,
                "Repeated formatter/cached hook does not duplicate dislikes");
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder(REGULAR), original) == replacement,
                "Unchanged bind reuses cached span");
        check(((Spanned) replacement).getSpans(0, replacement.length(), ForegroundColorSpan.class).length > 0,
                "Text color styling survives replacement");
        check(ReturnYouTubeDislike.getFetchForVideoId("pending") == data, "Repeated video ID reuses fetch");
        Spanned rolling = data.getDislikesSpanForRegularVideo(original, true, true);
        check(rolling.getSpans(0, rolling.length(), ImageSpan.class).length == 1,
                "Rolling-number cache does not reuse static separator geometry");

        data.setUserVote(ReturnYouTubeDislike.Vote.LIKE_REMOVE);
        check(data.getDislikesSpanForRegularVideo(original, true, false).toString().contains("123"),
                "Vote removal invalidates cached span without accumulating counts");
        settings.put("you_tube_settings.ryd_enabled", false);
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder(REGULAR), original) == original,
                "Disabled RYD leaves text unchanged");
        settings.remove("you_tube_settings.ryd_enabled");
        settings.put("you_tube_settings.ryd_dislike_percentage", true);
        check(data.getDislikesSpanForRegularVideo(original, true, false).toString().contains("%"),
                "Percentage toggle takes effect without restarting or external invalidation");
        settings.remove("you_tube_settings.ryd_dislike_percentage");
        check(!data.getDislikesSpanForRegularVideo(original, true, false).toString().contains("%"),
                "Percentage can be disabled without stale cached text");
        settings.put("you_tube_settings.ryd_compact_layout", true);
        Spanned compact = data.getDislikesSpanForRegularVideo(original, true, false);
        check(compact.getSpans(0, compact.length(), ImageSpan.class).length == 1,
                "Compact toggle removes left separator on the next bind");
        settings.remove("you_tube_settings.ryd_compact_layout");
        Spanned expanded = data.getDislikesSpanForRegularVideo(original, true, false);
        check(expanded.getSpans(0, expanded.length(), ImageSpan.class).length == 2,
                "Disabling compact restores left separator");
        SpannableString hidden = new SpannableString("Like");
        check(data.getDislikesSpanForRegularVideo(hidden, true, false).toString().equals("Likes hidden"),
                "Hidden likes respected by default");
        settings.put("you_tube_settings.ryd_estimated_like", true);
        check(data.getDislikesSpanForRegularVideo(hidden, true, false).toString().contains("123"),
                "Estimated-like toggle invalidates hidden-likes cache");
        settings.remove("you_tube_settings.ryd_estimated_like");

        ReturnYouTubeDislike next = fetched("next", 500, 42);
        set(ReturnYouTubeDislikePatch.class, null, "currentVideoData", next);
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder(REGULAR), original).toString().contains("42"),
                "Video switch uses next video's count");
        next.setVideoIdIsShort(true);
        check(next.getDislikesSpanForRegularVideo(original, true, false) == original,
                "Short data cannot decorate minimized regular video");
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder("|shorts_dislike_button.eml"), "Dislike")
                .toString().equals("42"), "Shorts accepts a plain String and renders count");
        settings.put("you_tube_settings.ryd_shorts", false);
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder("|shorts_dislike_button.eml"), "Dislike")
                .toString().equals("Dislike"), "Shorts toggle preserves original");
        settings.remove("you_tube_settings.ryd_shorts");
        ReturnYouTubeDislikePatch.setLastLithoShortsVideoId("pending");
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder("|shorts_like_button.eml"), "1.2K")
                .toString().equals("1.2K"), "Visible Shorts likes stay unchanged");
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder("|shorts_dislike_button.eml"), "Dislike")
                .toString().equals("123"), "Prefetched Shorts count belongs to upcoming Short");
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder("|shorts_dislike_button.eml"), "Dislike")
                .toString().equals("42"), "After prefetch pair, binds use current Short");
        ReturnYouTubeDislikePatch.setLastLithoShortsVideoId(null);
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder(REGULAR), original) == original,
                "Missing Shorts ID clears stale video association");

        ReturnYouTubeDislikeAPI.resetRateLimits();
        Response failed = new Response(500, "");
        responses.add(failed);
        ReturnYouTubeDislike unavailable = ReturnYouTubeDislike.getFetchForVideoId("failed");
        check(failed.closed.await(2, TimeUnit.SECONDS), "Failed fetch disconnected");
        check(unavailable.getDislikesSpanForRegularVideo(original, true, false) == original,
                "Failed fetch leaves original span");
        // disconnect() precedes publication of failure backoff; wait for that publication
        // before resetting it, otherwise this test races the production worker.
        Field backoff = ReturnYouTubeDislikeAPI.class.getDeclaredField("timeToResumeAPICalls");
        backoff.setAccessible(true);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (backoff.getLong(null) == 0 && System.nanoTime() < deadline) Thread.sleep(1);
        check(backoff.getLong(null) > 0, "Failed fetch publishes retry backoff");
        ReturnYouTubeDislikeAPI.resetRateLimits();
    }

    private static void requests() throws Exception {
        // Invoke all five endpoints through the production API, off Android's main thread.
        for (String endpoint : new String[]{"fetch", "register", "confirmRegistration", "vote", "confirmVote"}) {
            for (String mode : new String[]{"success", "http500", "rateLimit", "timeout", "io", "bodyFailure", "writeFailure"}) {
                if (mode.equals("bodyFailure") && endpoint.startsWith("confirm")) continue;
                if (mode.equals("writeFailure") && (endpoint.equals("fetch") || endpoint.equals("register"))) continue;
                ReturnYouTubeDislikeAPI.resetRateLimits();
                int status = mode.equals("http500") ? 500 : mode.equals("rateLimit") ? 429 : 200;
                String body = endpoint.equals("fetch") ? votes("request", 100, 10) : CHALLENGE;
                Response response = new Response(status, body);
                if (mode.equals("timeout")) response.failure = new SocketTimeoutException("simulated");
                if (mode.equals("io")) response.failure = new IOException("simulated");
                if (mode.equals("bodyFailure")) response.bodyFailure = true;
                if (mode.equals("writeFailure")) response.writeFailure = true;
                responses.add(response);
                Response confirmation = null;
                if (mode.equals("success") && (endpoint.equals("register") || endpoint.equals("vote"))) {
                    confirmation = new Response(200, "true");
                    responses.add(confirmation);
                }
                FutureTask<Object> request = new FutureTask<>(() -> invokeEndpoint(endpoint));
                new Thread(request).start();
                Object result = request.get(3, TimeUnit.SECONDS);
                check(mode.equals("success") == (result != null && !Boolean.FALSE.equals(result)),
                        endpoint + " " + mode + " result");
                check(response.closed.getCount() == 0, endpoint + " " + mode + " disconnects");
                if (confirmation != null) check(confirmation.closed.getCount() == 0, endpoint + " confirmation disconnects");
                if (response.streamOpened) check(response.streamClosed, endpoint + " " + mode + " closes input stream");
                check(responses.isEmpty(), endpoint + " " + mode + " consumes only expected requests");
            }
        }
        for (String body : new String[]{"{broken", votes("negative", -1, 0), "x".repeat(1_048_577)}) {
            ReturnYouTubeDislikeAPI.resetRateLimits();
            Response response = new Response(200, body);
            responses.add(response);
            FutureTask<RYDVoteData> invalid = new FutureTask<>(() -> ReturnYouTubeDislikeAPI.fetchVotes("invalid"));
            new Thread(invalid).start();
            check(invalid.get(2, TimeUnit.SECONDS) == null, "Malformed/negative/oversized response is contained (bytes=" + body.length() + ")");
            check(response.streamClosed && response.closed.getCount() == 0, "Invalid response closes stream and connection");
        }
        ReturnYouTubeDislikeAPI.resetRateLimits();
        responses.add(new Response(429, ""));
        FutureTask<Void> rateLimited = new FutureTask<>(() -> {
            check(ReturnYouTubeDislikeAPI.fetchVotes("limited") == null, "HTTP 429 returns no counts");
            check(ReturnYouTubeDislikeAPI.fetchVotes("limited-again") == null, "Rate-limit backoff blocks second network attempt");
            return null;
        });
        new Thread(rateLimited).start();
        rateLimited.get(2, TimeUnit.SECONDS);
        synchronized (opened) {
            check(opened.stream().allMatch(response -> response.closed.getCount() == 0), "Every opened HTTP connection disconnected");
        }
    }

    private static void concurrency() throws Exception {
        ReturnYouTubeDislike data = fetched("concurrent", 500, 42);
        List<FutureTask<Void>> workers = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            FutureTask<Void> task = new FutureTask<>(() -> {
                for (int j = 0; j < 100; j++) {
                    data.setUserVote((j & 1) == 0 ? ReturnYouTubeDislike.Vote.DISLIKE : ReturnYouTubeDislike.Vote.LIKE_REMOVE);
                    String span = data.getDislikesSpanForRegularVideo(new SpannableString("500"), true, (j & 1) == 0).toString();
                    if (!span.contains("42") && !span.contains("43")) throw new AssertionError("Invalid concurrent span: " + span);
                }
                return null;
            });
            workers.add(task);
            new Thread(task).start();
        }
        for (FutureTask<Void> task : workers) task.get(5, TimeUnit.SECONDS);
        check(true, "800 concurrent render/vote updates finish without deadlock or invalid counts");

        Response delayed = new Response(200, votes("old-late", 300, 17));
        delayed.release = new CountDownLatch(1);
        responses.add(delayed);
        ReturnYouTubeDislike old = ReturnYouTubeDislike.getFetchForVideoId("old-late");
        check(delayed.entered.await(2, TimeUnit.SECONDS), "Old video request in flight");
        ReturnYouTubeDislike next = fetched("new-first", 800, 88);
        set(ReturnYouTubeDislikePatch.class, null, "currentVideoData", next);
        delayed.release.countDown();
        awaitVotes(old);
        check(ReturnYouTubeDislikePatch.onLithoTextLoaded(new StringBuilder(REGULAR), new SpannableString("800"))
                .toString().contains("88"), "Late response cannot overwrite current video's count");

        set(ReturnYouTubeDislike.class, next, "timeFetched", System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(8));
        responses.add(new Response(200, votes("new-first", 800, 89)));
        ReturnYouTubeDislike refreshed = ReturnYouTubeDislike.getFetchForVideoId("new-first");
        check(refreshed != next, "Expired cache creates a new fetch");
        awaitVotes(refreshed);
        check(refreshed.getDislikesSpanForRegularVideo(new SpannableString("800"), true, false).toString().contains("89"),
                "Expired counts are replaced by fresh data");
    }

    private static void nativeLabelState() throws Exception {
        ReturnYouTubeDislike regular = fetched("badge-regular", 1000, 80);
        ReturnYouTubeDislike shorts = fetched("badge-short", 200, 20);
        Class<?> information = app.reseam.youtube.video.VideoInformation.class;
        set(information, null, "videoIdIsShort", false);
        set(information, null, "videoId", "badge-regular");
        DislikeLabel.newVideoLoaded("badge-regular");
        StringBuilder path = new StringBuilder("compactify_video_action_bar.eml|dislike_button_vm.eml|button_inner.eml|ContainerType|");
        CharSequence label = DislikeLabel.create(path);
        check(label instanceof Spanned, "Dislike label uses native Litho text spans");
        check(DislikeLabel.accessibilityText(label).toString().equals("80 dislikes"), "Native label accessibility exposes count");
        check(DislikeLabel.create(new StringBuilder("unrelated|ImageType|")) == null, "Unrelated icons unchanged");
        check(DislikeLabel.create(null) == null, "Missing native path unchanged");
        set(information, null, "videoIdIsShort", true);
        set(information, null, "videoId", "badge-short");
        DislikeLabel.newVideoLoaded("badge-short");
        check(DislikeLabel.accessibilityText(DislikeLabel.create(path)).toString().equals("80 dislikes"),
                "Shorts data cannot decorate regular button");
        check(DislikeLabel.accessibilityText(label).toString().equals("80 dislikes"), "Mounted label retains its video's count");
        settings.put("you_tube_settings.ryd_enabled", false);
        check(DislikeLabel.create(path) == null, "Disabled native label leaves original component");
        check(DislikeLabel.accessibilityText(label).toString().isEmpty(), "Disabled mounted label hides accessible count");
        settings.remove("you_tube_settings.ryd_enabled");
        android.graphics.drawable.Drawable drawable = ((Spanned) label).getSpans(0, label.length(), ImageSpan.class)[0].getDrawable();
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                drawable.getBounds().width(), drawable.getBounds().height(), android.graphics.Bitmap.Config.ARGB_8888);
        drawable.draw(new android.graphics.Canvas(bitmap));
        check(hasPixels(bitmap), "Native count draws within its measured span");
        bitmap.eraseColor(Color.TRANSPARENT);
        settings.put("you_tube_settings.ryd_enabled", false);
        drawable.draw(new android.graphics.Canvas(bitmap));
        check(!hasPixels(bitmap), "Disabled native count draws nothing");
        settings.remove("you_tube_settings.ryd_enabled");
        settings.put("you_tube_settings.ryd_dislike_percentage", true);
        check(DislikeLabel.accessibilityText(label).toString().contains("%"), "Native accessibility follows percentage setting");
        settings.remove("you_tube_settings.ryd_dislike_percentage");
        bitmap.recycle();
        AtomicInteger notifications = new AtomicInteger();
        app.reseam.youtube.player.Event.Observer<String> observer = id -> {
            if (id.equals("badge-regular")) notifications.incrementAndGet();
        };
        ReturnYouTubeDislike.onChange.add(observer);
        regular.setUserVote(ReturnYouTubeDislike.Vote.DISLIKE);
        check(notifications.get() == 1 && regular.getDislikeCountText().equals("81"),
                "Local vote notifies asynchronous label exactly once");
        ReturnYouTubeDislike.onChange.remove(observer);
        check(DislikeLabel.accessibilityText(label).toString().equals("81 dislikes"), "Mounted label reflects local vote without replacing span");
        CountDownLatch arrived = new CountDownLatch(1);
        app.reseam.youtube.player.Event.Observer<String> completion = id -> {
            if (id.equals("badge-async")) arrived.countDown();
        };
        ReturnYouTubeDislike.onChange.add(completion);
        ReturnYouTubeDislike async = fetched("badge-async", 100, 9);
        check(arrived.await(2, TimeUnit.SECONDS), "Completed fetch notifies label without a Litho rebind");
        check(async.getDislikeCountText().equals("9"), "Asynchronous label update reads completed count");
        ReturnYouTubeDislike.onChange.remove(completion);
        set(information, null, "videoIdIsShort", false);
    }

    private static boolean hasPixels(android.graphics.Bitmap bitmap) {
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) if (bitmap.getPixel(x, y) != Color.TRANSPARENT) return true;
        }
        return false;
    }

    private static Object get(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object invokeEndpoint(String endpoint) throws Exception {
        return switch (endpoint) {
            case "fetch" -> ReturnYouTubeDislikeAPI.fetchVotes("request");
            case "register" -> ReturnYouTubeDislikeAPI.registerAsNewUser();
            case "vote" -> ReturnYouTubeDislikeAPI.sendVote("request", ReturnYouTubeDislike.Vote.LIKE);
            case "confirmRegistration" -> invokePrivate("confirmRegistration", new String[]{"test-user", "test-solution"});
            case "confirmVote" -> invokePrivate("confirmVote", new String[]{"request", "test-user", "test-solution"});
            default -> throw new AssertionError(endpoint);
        };
    }

    private static Object invokePrivate(String name, String[] args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        java.util.Arrays.fill(types, String.class);
        Method method = ReturnYouTubeDislikeAPI.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, (Object[]) args);
    }

    private static ReturnYouTubeDislike fetched(String id, int likes, int dislikes) throws Exception {
        responses.add(new Response(200, votes(id, likes, dislikes)));
        ReturnYouTubeDislike data = ReturnYouTubeDislike.getFetchForVideoId(id);
        awaitVotes(data);
        return data;
    }

    private static void awaitVotes(ReturnYouTubeDislike data) throws Exception {
        Field field = ReturnYouTubeDislike.class.getDeclaredField("fetchedVotes");
        field.setAccessible(true);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (field.get(data) == null && System.nanoTime() < deadline) Thread.sleep(5);
        check(field.get(data) != null, "Background result published");
    }

    private static String votes(String id, int likes, int dislikes) {
        return "{\"id\":\"" + id + "\",\"viewCount\":10000,\"likes\":" + likes + ",\"dislikes\":" + dislikes + "}";
    }

    private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed++;
        System.out.println("PASS " + message);
    }

    private static Context testContext() {
        SharedPreferences preferences = (SharedPreferences) Proxy.newProxyInstance(
                RydDeviceRegression.class.getClassLoader(), new Class<?>[]{SharedPreferences.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getBoolean", "getString" -> settings.getOrDefault((String) args[0], args[1]);
                        case "contains" -> settings.containsKey(args[0]);
                        case "registerOnSharedPreferenceChangeListener", "unregisterOnSharedPreferenceChangeListener" -> null;
                        default -> throw new AssertionError("Unexpected preference access: " + method.getName());
                    };
                });
        return new ContextWrapper(null) {
            @Override public Context getApplicationContext() { return this; }
            @Override public Resources getResources() { return Resources.getSystem(); }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) { return preferences; }
        };
    }

    private static final class Response {
        final int status;
        final String body;
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        CountDownLatch release;
        IOException failure;
        boolean bodyFailure;
        boolean writeFailure;
        boolean streamOpened;
        boolean streamClosed;

        Response(int status, String body) { this.status = status; this.body = body; }

        HttpURLConnection connection(URL url) {
            return new HttpURLConnection(url) {
                @Override public void connect() {}
                @Override public boolean usingProxy() { return false; }
                @Override public void disconnect() { closed.countDown(); }
                @Override public int getResponseCode() throws IOException {
                    entered.countDown();
                    if (release != null) {
                        try {
                            if (!release.await(5, TimeUnit.SECONDS)) throw new SocketTimeoutException("Test release timed out");
                        } catch (InterruptedException ex) { throw new IOException(ex); }
                    }
                    if (failure != null) throw failure;
                    return status;
                }
                @Override public OutputStream getOutputStream() throws IOException {
                    if (writeFailure) throw new IOException("Simulated POST write failure");
                    return new ByteArrayOutputStream();
                }
                @Override public InputStream getInputStream() throws IOException {
                    if (bodyFailure) throw new IOException("Simulated body read failure");
                    streamOpened = true;
                    return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)) {
                        @Override public void close() throws IOException { streamClosed = true; super.close(); }
                    };
                }
            };
        }
    }
}
