// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.swipe;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.lang.ref.WeakReference;
import java.util.Locale;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.player.PlayerType;
import app.reseam.youtube.controls.PlayerControls;

/** Fullscreen gestures attached to the real activity, without replacing its superclass. */
public final class SwipeControls {
    private static WeakReference<State> current = new WeakReference<>(null);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static {
        PlayerType.onChange.add(type -> MAIN.post(() -> {
            State state = current.get();
            if (state != null) state.playerChanged(type);
        }));
    }

    private SwipeControls() {}

    public static void initialize(Activity activity) {
        State previous = current.get();
        if (previous != null && previous.activity == activity) return;
        State state = new State(activity);
        // The overlay owns the state for precisely the lifetime of the activity's view tree.
        state.overlay.setTag(state);
        current = new WeakReference<>(state);
        ((ViewGroup) activity.getWindow().getDecorView()).addView(state.overlay,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public static boolean touch(Activity activity, MotionEvent event) {
        State state = current.get();
        if (state == null || state.activity != activity || state.forwarding || event == null) return false;
        try { return state.touch(event); }
        catch (RuntimeException ex) {
            state.eligible = state.captured = false;
            Logger.error(() -> "Swipe controls: " + ex);
            return false;
        }
    }

    public static boolean key(Activity activity, KeyEvent event) {
        State state = current.get();
        if (state == null || state.activity != activity || event == null || !fullscreen()
                || !Settings.getBoolean("swipe_volume", true)) return false;
        int key = event.getKeyCode();
        if (key != KeyEvent.KEYCODE_VOLUME_DOWN && key != KeyEvent.KEYCODE_VOLUME_UP) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int volume = state.audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                    + (key == KeyEvent.KEYCODE_VOLUME_UP ? 1 : -1);
            state.setVolume(volume);
        }
        return true;
    }

    private static boolean fullscreen() {
        return PlayerType.current() == PlayerType.WATCH_WHILE_FULLSCREEN;
    }

    private static float number(String key, float fallback, float min, float max) {
        try {
            float value = Float.parseFloat(Settings.getString(key, Float.toString(fallback)));
            return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
        } catch (NumberFormatException ignored) { return fallback; }
    }

    private static final class State {
        final Activity activity;
        final AudioManager audio;
        final Overlay overlay;
        final float density;
        boolean eligible, captured, forwarding, volumeZone, brightnessChanged;
        float startX, startY, startBrightness, savedBrightness = -1, originalBrightness = -1;
        int startVolume;
        long downTime;
        final Runnable hide;
        final Runnable engagePress;

        State(Activity activity) {
            this.activity = activity;
            audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            density = activity.getResources().getDisplayMetrics().density;
            overlay = new Overlay(activity);
            overlay.setVisibility(View.GONE);
            overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            overlay.setClickable(false);
            hide = () -> overlay.setVisibility(View.GONE);
            engagePress = () -> {
                if (!eligible || !fullscreen()) return;
                MotionEvent cancel = MotionEvent.obtain(downTime, android.os.SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_CANCEL, startX, startY, 0);
                try { captureGesture(cancel); }
                finally { cancel.recycle(); }
            };
        }

        boolean touch(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                MAIN.removeCallbacks(engagePress);
                captured = false;
                startX = event.getX();
                startY = event.getY();
                downTime = event.getEventTime();
                View decor = activity.getWindow().getDecorView();
                float width = decor.getWidth(), height = decor.getHeight();
                volumeZone = startX > width * 0.625f;
                eligible = fullscreen() && event.getPointerCount() == 1
                        && startX > 20 * density && startX < width - 20 * density
                        && startY > 40 * density && startY < height - 80 * density
                        && (volumeZone || startX < width * 0.375f)
                        && Settings.getBoolean(volumeZone ? "swipe_volume" : "swipe_brightness", true)
                        && (Settings.getBoolean("swipe_press_to_engage", false) || !PlayerControls.isVisible());
                Logger.debug(() -> "Swipe gesture: " + PlayerType.current() + ", eligible=" + eligible
                        + ", controls=" + PlayerControls.isVisible());
                startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                startBrightness = activity.getWindow().getAttributes().screenBrightness;
                if (startBrightness < 0) {
                    startBrightness = android.provider.Settings.System.getInt(
                            activity.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, 128) / 255f;
                }
                if (eligible && Settings.getBoolean("swipe_press_to_engage", false)) {
                    MAIN.postDelayed(engagePress, ViewConfiguration.getLongPressTimeout());
                }
                return false;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || event.getPointerCount() > 1) {
                MAIN.removeCallbacks(engagePress);
            }
            if (!eligible) return captured;
            if (!fullscreen() || event.getPointerCount() > 1) {
                eligible = false;
                return captured; // A cancelled downstream gesture must not receive an orphan UP.
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                boolean consumed = captured;
                eligible = captured = false;
                return consumed;
            }
            if (action != MotionEvent.ACTION_MOVE) return captured;
            float dx = event.getX() - startX, dy = startY - event.getY();
            if (!captured) {
                // Claim vertical movement before YouTube opens its fullscreen recommendation
                // sheet. The configurable threshold controls adjustment, not event ownership.
                float threshold = ViewConfiguration.get(activity).getScaledTouchSlop();
                if (Math.abs(dx) > threshold && Math.abs(dx) > Math.abs(dy)) {
                    eligible = false;
                    MAIN.removeCallbacks(engagePress);
                    return false;
                }
                if (Math.abs(dy) < threshold || Math.abs(dy) <= Math.abs(dx)) return false;
                if (Settings.getBoolean("swipe_press_to_engage", false)
                        && event.getEventTime() - downTime < ViewConfiguration.getLongPressTimeout()) {
                    eligible = false;
                    MAIN.removeCallbacks(engagePress);
                    return false;
                }
                captureGesture(event);
            }
            if (Math.abs(dy) < number("swipe_threshold", 30, 5, 200) * density) return true;
            float fraction = dy / Math.max(1, activity.getWindow().getDecorView().getHeight() * 0.6f);
            if (volumeZone) {
                int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                float sensitivity = number("swipe_volume_sensitivity", 1, 0.1f, 10);
                setVolume(Math.round(startVolume + fraction * max * sensitivity));
            } else {
                if (!brightnessChanged) originalBrightness = activity.getWindow().getAttributes().screenBrightness;
                brightnessChanged = true;
                float brightness = Math.max(0, Math.min(1, startBrightness + fraction));
                if (brightness == 0 && Settings.getBoolean("swipe_lowest_value_enable_auto_brightness", false)) brightness = -1;
                setBrightness(brightness);
                savedBrightness = brightness;
                show("Brightness", Math.max(0, brightness), brightness < 0);
            }
            return true;
        }

        private void captureGesture(MotionEvent event) {
            if (captured) return;
            captured = true;
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            forwarding = true;
            try { activity.dispatchTouchEvent(cancel); }
            finally { forwarding = false; cancel.recycle(); }
            if (Settings.getBoolean("swipe_haptic_feedback", true)) {
                overlay.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        }

        void setVolume(int volume) {
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int min = audio.getStreamMinVolume(AudioManager.STREAM_MUSIC);
            volume = Math.max(min, Math.min(max, volume));
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            show("Volume", max == 0 ? 0 : (float) volume / max, false);
        }

        void setBrightness(float value) {
            WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
            attributes.screenBrightness = value;
            activity.getWindow().setAttributes(attributes);
        }

        void playerChanged(PlayerType type) {
            if (type != PlayerType.WATCH_WHILE_FULLSCREEN) {
                eligible = captured = false;
                MAIN.removeCallbacks(engagePress);
                MAIN.removeCallbacks(hide);
                hide.run();
                if (brightnessChanged && Settings.getBoolean("swipe_save_and_restore_brightness", true)) {
                    setBrightness(originalBrightness);
                }
            } else if (brightnessChanged && Settings.getBoolean("swipe_save_and_restore_brightness", true)) {
                setBrightness(savedBrightness);
            }
        }

        void show(String label, float value, boolean automatic) {
            overlay.label = label;
            overlay.value = value;
            overlay.automatic = automatic;
            overlay.setVisibility(View.VISIBLE);
            overlay.bringToFront();
            overlay.invalidate();
            MAIN.removeCallbacks(hide);
            MAIN.postDelayed(hide, (long) number("swipe_overlay_timeout", 1000, 100, 10000));
        }
    }

    private static final class Overlay extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        String label = "";
        float value;
        boolean automatic;

        Overlay(Context context) { super(context); }

        @Override protected void onDraw(Canvas canvas) {
            float density = getResources().getDisplayMetrics().density;
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            String style = Settings.getString("swipe_overlay_style", "CIRCULAR");
            if ("NONE".equals(style)) return;
            int alpha = (int) (number("swipe_overlay_background_opacity", 70, 0, 100) * 2.55f);
            paint.setColor(Color.argb(alpha, 0, 0, 0));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(cx - 100 * density, cy - 65 * density, cx + 100 * density,
                    cy + 65 * density, 16 * density, 16 * density, paint);
            int color = Color.WHITE;
            try { color = Color.parseColor(Settings.getString(
                    "Volume".equals(label) ? "swipe_overlay_progress_volume_color" : "swipe_overlay_progress_brightness_color",
                    "#FFFFFFFF")); } catch (IllegalArgumentException ignored) {}
            paint.setColor(color);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(number("swipe_text_overlay_size", 22, 10, 48) * getResources().getDisplayMetrics().scaledDensity);
            canvas.drawText(label, cx, cy - 12 * density, paint);
            canvas.drawText(automatic ? "Auto" : String.format(Locale.getDefault(), "%d%%", Math.round(value * 100)),
                    cx, cy + 18 * density, paint);
            if ("CIRCULAR".equals(style)) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4 * density);
                canvas.drawArc(cx - 55 * density, cy - 55 * density, cx + 55 * density,
                        cy + 55 * density, -90, 360 * value, false, paint);
            } else if ("PROGRESS".equals(style)) {
                canvas.drawRect(cx - 80 * density, cy + 40 * density,
                        cx - 80 * density + 160 * density * value, cy + 44 * density, paint);
            }
        }
    }
}
