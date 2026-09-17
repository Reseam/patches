// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.speed;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.icu.text.NumberFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.youtube.controls.SheetDialog;
import app.reseam.youtube.controls.Ui;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;
import app.reseam.youtube.player.Event;
import app.reseam.youtube.player.PlayerType;
import app.reseam.youtube.video.VideoInformation;

/** The configured speeds, the tap-and-hold speed, and the speed sheet that replaces YouTube's. */
public final class CustomPlaybackSpeed {
    /** Past 8x playback gets no faster and YouTube's own selector misbehaves. */
    public static final float PLAYBACK_SPEED_MAXIMUM = 8;
    /** The lower bound the patch gives YouTube's speed limiter, which then accepts any positive speed. */
    public static final float SPEED_LIMIT_MINIMUM = 0;
    private static final String SPEEDS_KEY = "custom_playback_speeds";
    private static final String DEFAULT_SPEEDS = "0.25 0.5 0.75 1.0 1.25 1.5 1.75 2.0 2.5 3.0 4.0 5.0 6.0 7.0 8.0";
    private static final String TAP_AND_HOLD_KEY = "playback_speed_tap_and_hold";
    private static final String DEFAULT_TAP_AND_HOLD = "2.0";
    private static final double SPEED_ADJUSTMENT_CHANGE = 0.05;
    /** Slider steps per 1.0x of speed. */
    private static final float PROGRESS_SCALE = 100;
    /** YouTube shows its old speed menu more than once per request; later calls inside this window are ignored. */
    private static final long OLD_MENU_DEBOUNCE_MILLISECONDS = 1000;

    public static final float[] customPlaybackSpeeds = loadCustomSpeeds();
    private static final float TAP_AND_HOLD_SPEED = loadTapAndHoldSpeed();
    private static final NumberFormat speedFormatter = NumberFormat.getNumberInstance();

    private static volatile long lastOldMenuTime;

    static {
        speedFormatter.setMinimumFractionDigits(2);
        speedFormatter.setMaximumFractionDigits(2);
    }

    private CustomPlaybackSpeed() {}

    /** Injection point. */
    public static int customPlaybackSpeedCount() {
        return customPlaybackSpeeds.length;
    }

    /** Injection point. */
    public static float getTapAndHoldSpeed() {
        return TAP_AND_HOLD_SPEED;
    }

    private static float loadTapAndHoldSpeed() {
        try {
            float speed = Float.parseFloat(Settings.getString(TAP_AND_HOLD_KEY, DEFAULT_TAP_AND_HOLD));
            if (speed > 0 && speed <= PLAYBACK_SPEED_MAXIMUM) return speed;
        } catch (NumberFormatException ignored) {
            // Falls through to the reset below, like an out-of-range value.
        }
        showInvalidSpeedToast();
        ReseamSettings.setString("you_tube_settings." + TAP_AND_HOLD_KEY, DEFAULT_TAP_AND_HOLD);
        return Float.parseFloat(DEFAULT_TAP_AND_HOLD);
    }

    private static float[] loadCustomSpeeds() {
        try {
            return parseSpeeds(Settings.getString(SPEEDS_KEY, DEFAULT_SPEEDS));
        } catch (IllegalArgumentException exception) {
            Logger.info(() -> "Invalid custom playback speeds: " + exception);
            showInvalidSpeedToast();
            ReseamSettings.setString("you_tube_settings." + SPEEDS_KEY, DEFAULT_SPEEDS);
            return parseSpeeds(DEFAULT_SPEEDS);
        }
    }

    private static float[] parseSpeeds(String raw) {
        String[] values = raw.trim().replace(',', '.').split("\\s+");
        float[] speeds = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            float speed = Float.parseFloat(values[i]);
            if (!(speed > 0 && speed <= PLAYBACK_SPEED_MAXIMUM) || contains(speeds, i, speed)) {
                throw new IllegalArgumentException(values[i]);
            }
            speeds[i] = speed;
        }
        Arrays.sort(speeds);
        return speeds;
    }

    private static boolean contains(float[] speeds, int count, float speed) {
        for (int i = 0; i < count; i++) if (speeds[i] == speed) return true;
        return false;
    }

    private static void showInvalidSpeedToast() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(YouTubeContext.get(),
                "Use unique playback speeds greater than 0 and at most " + PLAYBACK_SPEED_MAXIMUM,
                Toast.LENGTH_LONG).show());
    }

    /** Injection point, for every Litho recycler view: YouTube's speed menu is closed and this sheet shown instead. */
    public static void onFlyoutMenuCreate(View recyclerView) {
        if (!(recyclerView instanceof ViewGroup)) return;
        ViewGroup menu = (ViewGroup) recyclerView;
        menu.getViewTreeObserver().addOnDrawListener(() -> {
            if (PlaybackSpeedMenuFilter.playbackRateSelectorMenuVisible && replaceLithoMenu(menu, 5)) {
                PlaybackSpeedMenuFilter.playbackRateSelectorMenuVisible = false;
            }
            if (PlaybackSpeedMenuFilter.oldPlaybackSpeedMenuVisible && replaceLithoMenu(menu, 8)) {
                PlaybackSpeedMenuFilter.oldPlaybackSpeedMenuVisible = false;
            }
        });
    }

    private static boolean replaceLithoMenu(ViewGroup recyclerView, int expectedChildCount) {
        if (recyclerView.getChildCount() == 0
                || !(recyclerView.getChildAt(0) instanceof ViewGroup)
                || ((ViewGroup) recyclerView.getChildAt(0)).getChildCount() != expectedChildCount) {
            return false;
        }
        View third = ancestor(recyclerView, 3);
        if (!(third instanceof ViewGroup) || !(third.getParent() instanceof ViewGroup)) return false;
        ViewGroup fourth = (ViewGroup) third.getParent();

        // On phones the first child of the fourth ancestor is the sheet's touch-outside view.
        View touchOutside = fourth.getChildAt(0);
        if (touchOutside != null) {
            touchOutside.setSoundEffectsEnabled(false);
            touchOutside.performClick();
        }
        // Tablets have no such view, so both ancestors are hidden as well.
        third.setVisibility(View.GONE);
        fourth.setVisibility(View.GONE);

        if (Settings.getBoolean("restore_old_playback_speed_menu", false)) {
            showOldPlaybackSpeedMenu();
            Logger.debug(() -> "Old playback speed menu shown");
        } else {
            showModernDialog(recyclerView.getContext());
            Logger.debug(() -> "Custom playback speed dialog shown");
        }
        return true;
    }

    private static View ancestor(View view, int levels) {
        View current = view;
        for (int i = 0; i < levels && current != null; i++) {
            current = current.getParent() instanceof View ? (View) current.getParent() : null;
        }
        return current;
    }

    /** Opens YouTube's old speed menu. */
    public static void showOldPlaybackSpeedMenu() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastOldMenuTime < OLD_MENU_DEBOUNCE_MILLISECONDS) {
            Logger.debug(() -> "Ignoring a repeated old playback speed menu call");
            return;
        }
        lastOldMenuTime = now;
        openOldPlaybackSpeedMenu();
    }

    /** The patch replaces this body with a call on the old speed menu it captured. */
    private static void openOldPlaybackSpeedMenu() {}

    /** A sheet with the current speed, a slider with 0.05x steps, and a button per configured speed. */
    public static void showModernDialog(Context context) {
        float minimum = customPlaybackSpeeds[0];
        float maximum = customPlaybackSpeeds[customPlaybackSpeeds.length - 1];
        int foreground = Ui.foregroundColor(context);
        int buttonColor = Ui.adjustBrightness(context, Ui.dialogBackgroundColor(context), 0.95f, 1.115f);
        LinearLayout content = SheetDialog.createContent(context);

        TextView currentSpeed = new TextView(context);
        currentSpeed.setText(formatSpeedX(VideoInformation.getPlaybackSpeed()));
        currentSpeed.setTextColor(foreground);
        currentSpeed.setTextSize(16);
        currentSpeed.setTypeface(Typeface.DEFAULT_BOLD);
        currentSpeed.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(0, Ui.dp(20), 0, 0);
        currentSpeed.setLayoutParams(textParams);
        content.addView(currentSpeed);

        LinearLayout sliderRow = new LinearLayout(context);
        sliderRow.setOrientation(LinearLayout.HORIZONTAL);
        sliderRow.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = adjustButton(context, false, foreground, buttonColor);
        Button plus = adjustButton(context, true, foreground, buttonColor);
        SeekBar slider = new SeekBar(context);
        slider.setFocusable(true);
        slider.setFocusableInTouchMode(true);
        slider.setMax(toProgress(maximum, minimum));
        slider.setProgress(toProgress(VideoInformation.getPlaybackSpeed(), minimum));
        slider.getProgressDrawable().setColorFilter(foreground, PorterDuff.Mode.SRC_IN);
        slider.getThumb().setColorFilter(foreground, PorterDuff.Mode.SRC_IN);
        slider.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        sliderRow.addView(minus);
        sliderRow.addView(slider);
        sliderRow.addView(plus);
        content.addView(sliderRow);

        SpeedSelection select = requested -> {
            float speed = roundSpeed(requested);
            if (VideoInformation.getPlaybackSpeed() == speed) return;
            currentSpeed.setText(formatSpeedX(speed));
            slider.setProgress(toProgress(speed, minimum));
            RememberPlaybackSpeed.userSelectedPlaybackSpeed(speed);
            VideoInformation.overridePlaybackSpeed(speed);
        };
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) select.apply(minimum + progress / PROGRESS_SCALE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        minus.setOnClickListener(v -> select.apply((float) (VideoInformation.getPlaybackSpeed() - SPEED_ADJUSTMENT_CHANGE)));
        plus.setOnClickListener(v -> select.apply((float) (VideoInformation.getPlaybackSpeed() + SPEED_ADJUSTMENT_CHANGE)));

        GridLayout presets = new GridLayout(context);
        presets.setColumnCount(5);
        presets.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        presets.setRowCount((int) Math.ceil(customPlaybackSpeeds.length / 5.0));
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridParams.setMargins(Ui.dp(4), Ui.dp(12), Ui.dp(4), Ui.dp(12));
        presets.setLayoutParams(gridParams);
        for (float speed : customPlaybackSpeeds) {
            presets.addView(presetCell(context, speed, foreground, buttonColor, select));
        }
        content.addView(presets);

        SheetDialog dialog = SheetDialog.create(context, content);
        Event.Observer<PlayerType> pictureInPicture = type -> {
            if (type == PlayerType.WATCH_WHILE_PICTURE_IN_PICTURE) dialog.dismiss();
        };
        PlayerType.onChange.add(pictureInPicture);
        dialog.setOnDismissListener(d -> PlayerType.onChange.remove(pictureInPicture));
        dialog.show();
    }

    private interface SpeedSelection {
        void apply(float speed);
    }

    private static View presetCell(Context context, float speed, int foreground, int buttonColor,
                                   SpeedSelection select) {
        FrameLayout cell = new FrameLayout(context);
        GridLayout.LayoutParams cellParams = new GridLayout.LayoutParams();
        cellParams.width = 0;
        cellParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        cellParams.setMargins(Ui.dp(4), 0, Ui.dp(4), 0);
        cellParams.height = Ui.dp(60);
        cell.setLayoutParams(cellParams);

        Button button = new Button(context, null, 0);
        NumberFormat presetFormatter = NumberFormat.getNumberInstance();
        presetFormatter.setMinimumFractionDigits(1);
        presetFormatter.setMaximumFractionDigits(2);
        button.setText(presetFormatter.format(speed));
        button.setTextColor(foreground);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(buttonColor));
        button.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4));
        button.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, Ui.dp(32), Gravity.CENTER));
        button.setOnClickListener(v -> select.apply(speed));
        cell.addView(button);

        if (speed == 1.0f) {
            TextView normal = new TextView(context);
            int label = context.getResources().getIdentifier("normal_playback_rate_label", "string", context.getPackageName());
            normal.setText(label);
            normal.setTextColor(foreground);
            normal.setTextSize(10);
            normal.setGravity(Gravity.CENTER);
            normal.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
            cell.addView(normal);
        }
        return cell;
    }

    private static Button adjustButton(Context context, boolean plus, int foreground, int background) {
        Button button = new Button(context, null, 0);
        button.setText("");
        button.setBackground(rounded(background));
        button.setForeground(new SymbolDrawable(plus, foreground));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.dp(36), Ui.dp(36));
        params.setMargins(Ui.dp(8), 0, Ui.dp(8), 0);
        button.setLayoutParams(params);
        return button;
    }

    private static ShapeDrawable rounded(int color) {
        ShapeDrawable background = new ShapeDrawable(new RoundRectShape(Ui.roundedCorners(20), null, null));
        background.getPaint().setColor(color);
        return background;
    }

    private static String formatSpeedX(float speed) {
        return speedFormatter.format(speed) + 'x';
    }

    private static int toProgress(float speed, float minimum) {
        return (int) ((speed - minimum) * PROGRESS_SCALE);
    }

    /** The nearest 0.05x step, unless the speed is one of the configured ones exactly. */
    private static float roundSpeed(float speed) {
        if (contains(customPlaybackSpeeds, customPlaybackSpeeds.length, speed)) return speed;
        // Doubles, so a value like 1.15 does not round down to 1.1.
        float rounded = (float) (Math.round(speed / SPEED_ADJUSTMENT_CHANGE) * SPEED_ADJUSTMENT_CHANGE);
        return Math.max((float) SPEED_ADJUSTMENT_CHANGE, Math.min(PLAYBACK_SPEED_MAXIMUM, rounded));
    }

    /** An outlined plus or minus. */
    private static final class SymbolDrawable extends Drawable {
        private final boolean plus;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        SymbolDrawable(boolean plus, int color) {
            this.plus = plus;
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Ui.dp(1));
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float centerX = bounds.width() / 2f;
            float centerY = bounds.height() / 2f;
            float size = Math.min(bounds.width(), bounds.height()) * 0.25f;
            canvas.drawLine(centerX - size, centerY, centerX + size, centerY, paint);
            if (plus) canvas.drawLine(centerX, centerY - size, centerX, centerY + size, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
