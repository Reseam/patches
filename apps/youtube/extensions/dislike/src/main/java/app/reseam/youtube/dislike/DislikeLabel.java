// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later
package app.reseam.youtube.dislike;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.View;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;
import app.reseam.youtube.video.VideoInformation;
import app.reseam.runtime.settings.ReseamSettings;

/** A label mounted by Litho inside the existing dislike button, never in a window overlay. */
public final class DislikeLabel extends Drawable {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<DislikeLabel> labels = Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile String regularVideoId = "";
    private static final SharedPreferences.OnSharedPreferenceChangeListener SETTINGS = (prefs, key) -> {
        if (key == null || key.startsWith("you_tube_settings.ryd_")) refresh(null);
    };
    static {
        ReturnYouTubeDislike.onChange.add(DislikeLabel::refresh);
        SharedPreferences prefs = ReseamSettings.prefs();
        if (prefs != null) prefs.registerOnSharedPreferenceChangeListener(SETTINGS);
    }
    private static void refresh(String id) {
        MAIN.post(() -> {
            synchronized (labels) {
                for (DislikeLabel label : labels) {
                    if (id == null || label.data.getVideoId().equals(id)) {
                        label.updateAccessibility();
                        label.invalidateSelf();
                    }
                }
            }
        });
    }
    public static void newVideoLoaded(String id) {
        if (id != null && !id.isEmpty() && !VideoInformation.lastVideoIdIsShort()) regularVideoId = id;
    }
    private final ReturnYouTubeDislike data;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private WeakReference<View> accessibilityHost = new WeakReference<>(null);
    private String lastDescription;

    private DislikeLabel(ReturnYouTubeDislike data) {
        this.data = data;
        android.util.DisplayMetrics metrics = YouTubeContext.get().getResources().getDisplayMetrics();
        paint.setTextSize(11 * metrics.scaledDensity);
        paint.setTextAlign(Paint.Align.CENTER);
        setBounds(0, 0, Math.max(Math.round(40 * metrics.density), (int) Math.ceil(paint.measureText("99.9M") + 4 * metrics.density)),
                Math.max(Math.round(14 * metrics.density), (int) Math.ceil(paint.descent() - paint.ascent())));
        synchronized (labels) { labels.add(this); }
    }

    public static CharSequence create(StringBuilder path) {
        if (path == null || !Settings.getBoolean("ryd_enabled", true)
                || path.indexOf("compactify_video_action_bar.e") != 0
                || path.indexOf("|dislike_button_vm.e") < 0
                || path.indexOf("|button_inner.e") < 0
                || path.lastIndexOf("|ContainerType|") != path.length() - "|ContainerType|".length()) return null;
        String id = regularVideoId;
        if (id.isEmpty()) return null;
        DislikeLabel label = new DislikeLabel(ReturnYouTubeDislike.getFetchForVideoId(id));
        SpannableString text = new SpannableString("Dislikes");
        text.setSpan(new ImageSpan(label), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    public static List<Object> children(Object icon, Object text) { return Arrays.asList(icon, text); }
    public static int iconHeight() {
        return Math.round(32 * YouTubeContext.get().getResources().getDisplayMetrics().density);
    }
    public static int withFlags(int flags, int mask) { return flags | mask; }

    public static CharSequence accessibilityText(CharSequence text) {
        if (text instanceof Spanned) {
            for (ImageSpan span : ((Spanned) text).getSpans(0, text.length(), ImageSpan.class)) {
                if (span.getDrawable() instanceof DislikeLabel) {
                    if (!Settings.getBoolean("ryd_enabled", true)) return "";
                    String count = ((DislikeLabel) span.getDrawable()).data.getDislikeCountText();
                    return count == null ? "Dislikes unavailable" : count + " dislikes";
                }
            }
        }
        return text;
    }

    @Override public void draw(Canvas canvas) {
        updateAccessibility();
        if (!Settings.getBoolean("ryd_enabled", true)) return;
        String count = data.getDislikeCountText();
        paint.setColor(RuntimeUtils.isDarkModeEnabled() ? 0xFFAAAAAA : 0xFF606060);
        canvas.drawText(count == null ? "—" : count, getBounds().exactCenterX(),
                getBounds().exactCenterY() - (paint.ascent() + paint.descent()) / 2, paint);
    }
    private void updateAccessibility() {
        if (Looper.myLooper() != Looper.getMainLooper()) return;
        // Litho wraps this text in its own host. Follow drawable ownership, not the view tree.
        Drawable.Callback callback = getCallback();
        for (int depth = 0; callback instanceof Drawable && depth < 8; depth++) {
            callback = ((Drawable) callback).getCallback();
        }
        if (!(callback instanceof View)) return;
        View host = (View) callback;
        boolean enabled = Settings.getBoolean("ryd_enabled", true);
        String count = data.getDislikeCountText();
        String description = !enabled ? "" : count == null ? "Dislikes unavailable" : count + " dislikes";
        if (host == accessibilityHost.get() && description.equals(lastDescription)) return;
        accessibilityHost = new WeakReference<>(host);
        lastDescription = description;
        host.setImportantForAccessibility(enabled ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        host.setContentDescription(description);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
