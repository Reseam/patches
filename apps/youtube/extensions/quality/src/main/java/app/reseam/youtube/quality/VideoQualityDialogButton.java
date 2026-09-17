// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.quality;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import app.reseam.youtube.controls.PlayerControlButton;
import app.reseam.youtube.controls.SheetDialog;
import app.reseam.youtube.controls.Ui;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.player.Event;
import app.reseam.youtube.player.PlayerType;
import app.reseam.youtube.video.VideoInformation;
import app.reseam.youtube.video.VideoInformation.VideoQualityInterface;

/** A bottom-player button labelled with the current quality; it opens a quality sheet and resets on long press. */
public final class VideoQualityDialogButton {
    /** The overlay settles on the last quality YouTube reports, not on every intermediate one. */
    private static final long TEXT_UPDATE_DELAY_MILLISECONDS = 100;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static PlayerControlButton button;
    private static CharSequence pendingText;

    static {
        VideoInformation.onQualityChange.add(VideoQualityDialogButton::updateText);
    }

    private VideoQualityDialogButton() {}

    public static void initialize(View controls) {
        try {
            button = new PlayerControlButton(controls,
                    "reseam_video_quality_button_container",
                    "reseam_video_quality_button",
                    "reseam_video_quality_button_text",
                    () -> Settings.getBoolean("video_quality_dialog_button", false),
                    view -> show(view.getContext()),
                    view -> {
                        resetQuality(view.getContext());
                        return true;
                    });
            updateText(VideoInformation.getCurrentQuality());
            Logger.debug(() -> "Video quality dialog button initialized");
        } catch (Exception exception) {
            Logger.error(() -> "Video quality dialog button initialization failure: " + exception);
        }
    }

    /** Picks the highest quality within the default resolution; with no such quality the sheet opens instead. */
    private static void resetQuality(Context context) {
        VideoQualityInterface[] qualities = VideoInformation.getCurrentQualities();
        if (qualities == null) return;
        int defaultResolution = RememberVideoQuality.getDefaultQualityResolution();
        for (VideoQualityInterface quality : qualities) {
            if (quality == null) continue;
            int resolution = quality.patch_getResolution();
            if (resolution != VideoInformation.AUTOMATIC_VIDEO_QUALITY_VALUE && resolution <= defaultResolution) {
                Logger.debug(() -> "Resetting quality to: " + quality.patch_getQualityName());
                VideoInformation.changeQuality(quality);
                return;
            }
        }
        // The quality hook cannot select automatic quality.
        show(context);
    }

    private static void updateText(VideoQualityInterface quality) {
        int resolution = quality == null ? VideoInformation.AUTOMATIC_VIDEO_QUALITY_VALUE : quality.patch_getResolution();
        String label;
        switch (resolution) {
            case VideoInformation.AUTOMATIC_VIDEO_QUALITY_VALUE: label = ""; break;
            case 144: case 240: case 360: label = "LD"; break;
            case 480: label = "SD"; break;
            case 720: label = "HD"; break;
            case 1080: label = "FHD"; break;
            case 1440: label = "QHD"; break;
            case 2160: label = "4K"; break;
            default: label = "?";
        }
        SpannableStringBuilder text = new SpannableStringBuilder(label);
        if (VideoInformation.isPremiumVideoQuality(quality)) {
            text.setSpan(new UnderlineSpan(), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        MAIN.post(() -> {
            pendingText = text;
            MAIN.postDelayed(() -> {
                if (pendingText == text && button != null) button.setTextOverlay(text);
            }, TEXT_UPDATE_DELAY_MILLISECONDS);
        });
    }

    private static void show(Context context) {
        VideoQualityInterface[] qualities = VideoInformation.getCurrentQualities();
        VideoQualityInterface current = VideoInformation.getCurrentQuality();
        if (qualities == null || current == null || qualities.length < 2) {
            Logger.debug(() -> "No qualities to show yet");
            return;
        }
        // Keep each displayed row paired with its actual quality; automatic may appear anywhere.
        List<String> labels = new ArrayList<>(qualities.length);
        List<VideoQualityInterface> choices = new ArrayList<>(qualities.length);
        int selected = -1;
        for (VideoQualityInterface quality : qualities) {
            if (quality == null || quality.patch_getResolution() == VideoInformation.AUTOMATIC_VIDEO_QUALITY_VALUE) continue;
            if (quality.patch_getQualityName().equals(current.patch_getQualityName())) selected = labels.size();
            labels.add(quality.patch_getQualityName());
            choices.add(quality);
        }

        int foreground = Ui.foregroundColor(context);
        int dimmed = Ui.adjustBrightness(context, foreground, 1.6f, 0.6f);
        LinearLayout content = SheetDialog.createContent(context);
        SpannableStringBuilder title = new SpannableStringBuilder();
        appendColored(title, youTubeString(context, "video_quality_quick_menu_title"), foreground);
        title.append("   ");
        appendColored(title, youTubeString(context, "video_quality_title_seperator"), dimmed);
        title.append("   ");
        appendColored(title, current.patch_getQualityName(), dimmed);
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(16);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(Ui.dp(12), Ui.dp(16), 0, Ui.dp(16));
        titleView.setLayoutParams(titleParams);
        content.addView(titleView);

        ListView list = new ListView(context);
        list.setAdapter(new QualityAdapter(context, labels, selected, foreground));
        list.setDivider(null);
        content.addView(list);

        SheetDialog dialog = SheetDialog.create(context, content);
        list.setOnItemClickListener((parent, view, position, id) -> {
            VideoQualityInterface quality = choices.get(position);
            RememberVideoQuality.userChangedQuality(quality.patch_getResolution());
            VideoInformation.changeQuality(quality);
            dialog.dismiss();
        });
        Event.Observer<PlayerType> pictureInPicture = type -> {
            if (type == PlayerType.WATCH_WHILE_PICTURE_IN_PICTURE) dialog.dismiss();
        };
        PlayerType.onChange.add(pictureInPicture);
        dialog.setOnDismissListener(d -> PlayerType.onChange.remove(pictureInPicture));
        dialog.show();
    }

    private static void appendColored(SpannableStringBuilder builder, String text, int color) {
        int start = builder.length();
        builder.append(text);
        builder.setSpan(new ForegroundColorSpan(color), start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static String youTubeString(Context context, String name) {
        return context.getString(context.getResources().getIdentifier(name, "string", context.getPackageName()));
    }

    /** Rows with a check mark in front of the selected quality. */
    private static final class QualityAdapter extends ArrayAdapter<String> {
        private final int selected;
        private final int foreground;

        QualityAdapter(Context context, List<String> labels, int selected, int foreground) {
            super(context, 0, labels);
            this.selected = selected;
            this.foreground = foreground;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = getContext();
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(16), Ui.dp(12));

            ImageView check = new ImageView(context);
            check.setImageResource(context.getResources().getIdentifier(
                    "quantum_ic_check_white_24", "drawable", context.getPackageName()));
            check.setColorFilter(foreground, PorterDuff.Mode.SRC_IN);
            check.setVisibility(position == selected ? View.VISIBLE : View.INVISIBLE);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24));
            checkParams.setMarginEnd(Ui.dp(24));
            row.addView(check, checkParams);

            TextView text = new TextView(context);
            text.setText(getItem(position));
            text.setTextAppearance(android.R.style.TextAppearance_Material_Subhead);
            text.setTextColor(foreground);
            row.addView(text);
            return row;
        }
    }
}
