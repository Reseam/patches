// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Locale;

import app.reseam.youtube.core.Logger;

/**
 * A small frame card above the seekbar while scrubbing the watch player, fed by the events and
 * storyboard frames YouTube sends to its full-size preview.
 */
public final class ScrubbingPreview {
    private static final int SCRUB_STARTED = 1;
    private static final int SCRUB_MOVED = 2;

    private static final Handler mainThread = new Handler(Looper.getMainLooper());
    private static final Point scrubber = new Point();
    private static final int[] seekbarOrigin = new int[2];
    private static final int[] hostOrigin = new int[2];

    private static WeakReference<View> seekbar = new WeakReference<>(null);
    private static Card card;
    private static boolean scrubbing;
    private static boolean filmStripOpen;
    private static long scrubTime;

    private ScrubbingPreview() {}

    /** Injection point, from the seekbar's draw pass. The seekbar being scrubbed always redraws. */
    public static void setSeekbar(View view) {
        if (seekbar.get() != view) seekbar = new WeakReference<>(view);
    }

    /** Injection point, with the full-size preview's scrub state and the position in milliseconds. */
    public static void onScrub(int state, long timeMillis) {
        try {
            if (state != SCRUB_STARTED && state != SCRUB_MOVED) {
                scrubbing = false;
                if (card != null) card.detach();
                return;
            }
            scrubbing = true;
            scrubTime = timeMillis;
            if (card != null && card.hasFrame()) show();
        } catch (Exception e) {
            Logger.error(() -> "Scrubbing preview failed", e);
        }
    }

    /** Injection point, with the storyboard frame for the latest scrub position. Not always on the main thread. */
    public static void onFrame(Bitmap frame) {
        if (frame == null) return;
        mainThread.post(() -> {
            try {
                View view = seekbar.get();
                if (!scrubbing || view == null) return;
                if (card == null || card.context != view.getContext()) {
                    if (card != null) card.detach();
                    card = new Card(view.getContext());
                }
                card.setFrame(frame);
                show();
            } catch (Exception e) {
                Logger.error(() -> "Scrubbing preview frame failed", e);
            }
        });
    }

    /** Injection point. Precise seeking's film strip takes the space above the seekbar while open. */
    public static void setFilmStripState(Enum<?> state) {
        filmStripOpen = !"CLOSED".equals(state.name());
        if (filmStripOpen && card != null) card.detach();
    }

    /** The seekbar's own scrubber position, in its parent's coordinates. The patch writes the body. */
    private static void scrubberPosition(View seekbar, Point out) {}

    private static void show() {
        View view = seekbar.get();
        if (filmStripOpen || view == null || !view.isAttachedToWindow()) {
            card.detach();
            return;
        }
        card.setTime(scrubTime);

        scrubberPosition(view, scrubber);
        ViewGroup host = (ViewGroup) view.getRootView();
        view.getLocationInWindow(seekbarOrigin);
        host.getLocationInWindow(hostOrigin);
        int left = seekbarOrigin[0] - hostOrigin[0];
        int top = seekbarOrigin[1] - hostOrigin[1];
        int x = left + scrubber.x - view.getLeft();
        int y = top + scrubber.y - view.getTop();

        card.attach(host);
        int width = card.root.getMeasuredWidth();
        int height = card.root.getMeasuredHeight();
        int cardLeft = Math.max(left, Math.min(x - width / 2, left + view.getWidth() - width));
        int cardTop = y - Card.GAP - height;
        card.root.layout(cardLeft, cardTop, cardLeft + width, cardTop + height);
    }

    private static int dp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                Resources.getSystem().getDisplayMetrics());
    }

    /** Drawn in the window's root overlay, so it never takes touches or changes the player's layout. */
    private static final class Card {
        // Clears the most-replayed graph YouTube draws over the seekbar while scrubbing.
        static final int GAP = dp(28);
        static final int LONG_SIDE = dp(160);
        static final int CORNER = dp(8);

        final Context context;
        final LinearLayout root;
        final ImageView frame;
        final TextView time;
        ViewGroup host;
        Bitmap shown;

        Card(Context context) {
            this.context = context;

            frame = new ImageView(context);
            frame.setScaleType(ImageView.ScaleType.CENTER_CROP);
            frame.setClipToOutline(true);
            frame.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), CORNER);
                }
            });

            GradientDrawable pill = new GradientDrawable();
            pill.setColor(0xB3000000);
            pill.setCornerRadius(dp(12));
            time = new TextView(context);
            time.setTextColor(Color.WHITE);
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            time.setPadding(dp(8), dp(2), dp(8), dp(2));
            time.setBackground(pill);

            root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER_HORIZONTAL);
            root.addView(frame);
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            timeParams.topMargin = dp(4);
            root.addView(time, timeParams);
        }

        boolean hasFrame() {
            return shown != null;
        }

        void setFrame(Bitmap bitmap) {
            if (bitmap == shown) return;
            shown = bitmap;
            frame.setImageBitmap(bitmap);
            // Storyboards follow the video's shape, so a vertical video gets a tall card.
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            frame.setLayoutParams(width >= height
                    ? new LinearLayout.LayoutParams(LONG_SIDE, LONG_SIDE * height / width)
                    : new LinearLayout.LayoutParams(LONG_SIDE * width / height, LONG_SIDE));
        }

        void setTime(long millis) {
            long seconds = millis / 1000;
            time.setText(seconds >= 3600
                    ? String.format(Locale.getDefault(), "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                    : String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60));
        }

        void attach(ViewGroup target) {
            if (host != target) {
                detach();
                target.getOverlay().add(root);
                host = target;
            }
            int unbounded = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            root.measure(unbounded, unbounded);
        }

        void detach() {
            if (host != null) host.getOverlay().remove(root);
            host = null;
            shown = null;
            frame.setImageBitmap(null);
        }
    }
}
