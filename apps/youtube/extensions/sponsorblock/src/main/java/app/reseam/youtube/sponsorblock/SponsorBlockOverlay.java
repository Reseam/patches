// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;
import app.reseam.youtube.controls.PlayerControls;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.player.PlayerType;
import app.reseam.youtube.video.VideoInformation;

/** Native Android UI; feature settings are supplied by the Reseam settings DSL. */
final class SponsorBlockOverlay extends FrameLayout {
    private final Button skip, menu;
    private Segment displayed;
    private boolean undoVisible;
    private long shownAt;
    private long draftStart = -1, draftEnd = -1;
    private String draftVideo = "";
    private final Runnable dismiss;

    SponsorBlockOverlay(Activity activity) {
        super(activity);
        setClipChildren(false);
        skip = new Button(activity);
        dismiss = () -> { undoVisible = false; skip.setVisibility(GONE); };
        skip.setAllCaps(false);
        skip.setTextColor(Color.WHITE);
        LayoutParams skipLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        skipLayout.setMargins(dp(16), dp(16), dp(28), dp(60));
        addView(skip, skipLayout);
        menu = new Button(activity);
        menu.setText("SB");
        menu.setContentDescription("SponsorBlock segments, voting and submissions");
        LayoutParams menuLayout = new LayoutParams(dp(64), dp(48), Gravity.TOP | Gravity.END);
        menuLayout.setMargins(0, dp(68), dp(20), 0);
        addView(menu, menuLayout);
        menu.setOnClickListener(view -> showMenu());
        skip.setVisibility(GONE);
        menu.setVisibility(GONE);
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    /** Keep controls inside the real player, including portrait, rotation and split-screen. */
    void alignTo(View player) {
        if (!(getParent() instanceof View) || player.getWidth() == 0 || player.getHeight() == 0) return;
        int[] position = new int[2], parent = new int[2];
        player.getLocationOnScreen(position);
        ((View) getParent()).getLocationOnScreen(parent);
        android.view.ViewGroup.LayoutParams bounds = getLayoutParams();
        if (bounds.width != player.getWidth() || bounds.height != player.getHeight()) {
            bounds.width = player.getWidth();
            bounds.height = player.getHeight();
            setLayoutParams(bounds);
        }
        setTranslationX(position[0] - parent[0]);
        setTranslationY(position[1] - parent[1]);
    }

    void update(Segment segment, boolean active) {
        boolean playerVisible = PlayerType.current().isMaximizedOrFullscreen();
        menu.setVisibility(active && playerVisible && PlayerControls.isVisible()
                && (Settings.getBoolean("sb_voting_button", false) || Settings.getBoolean("sb_create_new_segment", false))
                ? VISIBLE : GONE);
        if (!active || !playerVisible) {
            removeCallbacks(dismiss);
            undoVisible = false;
            displayed = null;
            skip.setVisibility(GONE);
            return;
        }
        if (undoVisible) return;
        if (segment == null) {
            displayed = null;
            skip.setVisibility(GONE);
            return;
        }
        if (displayed != segment) {
            shownAt = android.os.SystemClock.elapsedRealtime();
            displayed = segment;
        }
        boolean hide = Settings.getBoolean("sb_auto_hide_skip_button", true)
                && android.os.SystemClock.elapsedRealtime() - shownAt > autoHideTime();
        skip.setVisibility(hide ? GONE : VISIBLE);
        skip.setText(Settings.getBoolean("sb_compact_skip_button", false) ? "Skip"
                : segment.highlight() ? "Jump to highlight" : "Skip " + segment.category.title.toLowerCase());
        styleButton();
        skip.setOnClickListener(view -> SponsorBlock.skip(segment));
    }

    void offerUndo(Segment segment, long time, long skippedTo) {
        if (!PlayerType.current().isMaximizedOrFullscreen()) return;
        undoVisible = true;
        removeCallbacks(dismiss);
        skip.setText("Undo skip");
        skip.setVisibility(VISIBLE);
        styleButton();
        skip.setOnClickListener(view -> {
            SponsorBlock.undo(segment, time, skippedTo);
            dismiss.run();
        });
        postDelayed(dismiss, autoHideTime());
    }

    private long autoHideTime() {
        try { return Math.max(1000, Math.min(15000, Long.parseLong(Settings.getString("sb_auto_hide_skip_button_duration", "4000")))); }
        catch (NumberFormatException ignored) { return 4000; }
    }

    private void styleButton() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xDD222222);
        background.setCornerRadius(Settings.getBoolean("sb_square_layout", false) ? 0 : dp(24));
        skip.setBackground(background);
    }

    private void showMenu() {
        List<String> labels = new ArrayList<>();
        labels.add("Segments and voting");
        if (Settings.getBoolean("sb_create_new_segment", false)) labels.add("Create a segment");
        labels.add("Statistics");
        labels.add("About SponsorBlock");
        new AlertDialog.Builder(getContext()).setTitle("SponsorBlock").setItems(labels.toArray(new String[0]),
                (dialog, which) -> {
                    switch (labels.get(which)) {
                        case "Segments and voting": showSegments(); break;
                        case "Create a segment": createSegment(); break;
                        case "Statistics": showStats(); break;
                        default: new AlertDialog.Builder(getContext()).setTitle("SponsorBlock")
                                .setMessage("Crowdsourced segments provided by SponsorBlock (sponsor.ajay.app). "
                                        + "Submissions and votes are public contributions. Never share your private user ID.")
                                .setPositiveButton("OK", null).show();
                    }
                }).show();
    }

    private void showSegments() {
        List<Segment> segments = SponsorBlock.currentSegments();
        String[] labels = new String[segments.size()];
        for (int i = 0; i < labels.length; i++) {
            Segment segment = segments.get(i);
            labels[i] = segment.category.title + ": " + DateUtils.formatElapsedTime(segment.start / 1000)
                    + "–" + DateUtils.formatElapsedTime(segment.end / 1000);
        }
        if (labels.length == 0) { SponsorBlockApi.toast("No segments for this video"); return; }
        new AlertDialog.Builder(getContext()).setTitle("Segments").setItems(labels, (dialog, which) -> {
            Segment segment = segments.get(which);
            new AlertDialog.Builder(getContext()).setTitle(labels[which])
                    .setItems(new String[]{"Jump to start", "Upvote", "Downvote", "Change category"}, (detail, action) -> {
                        if (action == 0) {
                            SponsorBlock.undo(segment, segment.start);
                        } else if (action == 3) {
                            chooseCategory(category -> vote(segment, "category", category.key));
                        } else {
                            vote(segment, "type", action == 1 ? "1" : "0");
                        }
                    }).show();
        }).show();
    }

    private void vote(Segment segment, String key, String value) {
        SponsorBlockApi.execute(() -> {
            try {
                SponsorBlockApi.request("POST", "/api/voteOnSponsorTime",
                        "userID", SponsorBlockApi.userId(), "UUID", segment.uuid, key, value);
                SponsorBlockApi.toast("SponsorBlock vote submitted");
            } catch (Exception error) { SponsorBlockApi.toast("Vote failed: " + error.getMessage()); }
        });
    }

    private interface CategoryCallback { void selected(Segment.Category category); }
    private void chooseCategory(CategoryCallback callback) {
        Segment.Category[] categories = Segment.Category.values();
        String[] labels = new String[categories.length];
        for (int i = 0; i < labels.length; i++) labels[i] = categories[i].title;
        new AlertDialog.Builder(getContext()).setTitle("Category").setItems(labels,
                (dialog, which) -> callback.selected(categories[which])).show();
    }

    private void createSegment() {
        String id = SponsorBlock.currentVideoId();
        if (!id.equals(draftVideo)) {
            draftVideo = id;
            draftStart = draftEnd = -1;
        }
        new AlertDialog.Builder(getContext()).setTitle("Create a segment")
                .setItems(new String[]{"Mark start here", "Mark end here", "Edit, preview and submit", "Discard"},
                        (dialog, which) -> {
                            if (which == 0) {
                                draftStart = VideoInformation.getVideoTime();
                                SponsorBlockApi.toast("Start marked");
                            } else if (which == 1) {
                                draftEnd = VideoInformation.getVideoTime();
                                SponsorBlockApi.toast("End marked");
                            } else if (which == 2) editDraft();
                            else draftStart = draftEnd = -1;
                        }).show();
    }

    private void editDraft() {
        LinearLayout fields = new LinearLayout(getContext());
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), 0, dp(20), 0);
        EditText from = timeField("Start (seconds)", draftStart, fields);
        EditText to = timeField("End (seconds)", draftEnd, fields);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setTitle("Segment times")
                .setView(fields).setNegativeButton("Cancel", null)
                .setNeutralButton("Preview", null).setPositiveButton("Choose category", null).create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                if (readDraft(from, to)) VideoInformation.seekTo(Math.max(0, draftStart - 2000));
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                if (!readDraft(from, to)) return;
                dialog.dismiss();
                chooseCategory(category -> {
                    long start = draftStart, end = category == Segment.Category.HIGHLIGHT ? draftStart : draftEnd;
                    String id = draftVideo;
                    long duration = VideoInformation.getVideoLength();
                    if (!id.equals(SponsorBlock.currentVideoId())) return;
                    if (end == start && category != Segment.Category.HIGHLIGHT) {
                        SponsorBlockApi.toast("Only highlights may have identical start and end times");
                        return;
                    }
                    new AlertDialog.Builder(getContext()).setTitle("Submit " + category.title + "?")
                            .setMessage("Check the category guidelines at sponsor.ajay.app. This publishes "
                                    + String.format(Locale.ROOT, "%.3f–%.3f seconds", start / 1000d, end / 1000d)
                                    + " for this video.")
                            .setNegativeButton("Cancel", null).setPositiveButton("Submit", (confirm, button) ->
                                SponsorBlockApi.execute(() -> {
                                    try {
                                        SponsorBlockApi.request("POST", "/api/skipSegments",
                                                "userID", SponsorBlockApi.userId(), "videoID", id,
                                                "category", category.key, "startTime", Double.toString(start / 1000d),
                                                "endTime", Double.toString(end / 1000d),
                                                "actionType", category == Segment.Category.HIGHLIGHT ? "poi" : "skip",
                                                "videoDuration", Double.toString(duration / 1000d));
                                        post(() -> {
                                            if (id.equals(draftVideo) && draftStart == start) draftStart = draftEnd = -1;
                                        });
                                        SponsorBlockApi.toast("Segment submitted");
                                    } catch (Exception error) { SponsorBlockApi.toast("Submission failed: " + error.getMessage()); }
                                })).show();
                });
            });
        });
        dialog.show();
    }

    private EditText timeField(String label, long time, LinearLayout parent) {
        EditText field = new EditText(getContext());
        field.setHint(label);
        field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (time >= 0) field.setText(String.format(Locale.ROOT, "%.3f", time / 1000d));
        parent.addView(field);
        return field;
    }

    private boolean readDraft(EditText from, EditText to) {
        try {
            double start = Double.parseDouble(from.getText().toString()), end = Double.parseDouble(to.getText().toString());
            if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end < start
                    || end * 1000 > VideoInformation.getVideoLength() || !draftVideo.equals(SponsorBlock.currentVideoId())) {
                throw new IllegalArgumentException();
            }
            draftStart = Math.round(start * 1000);
            draftEnd = Math.round(end * 1000);
            return true;
        } catch (IllegalArgumentException error) {
            SponsorBlockApi.toast("Enter valid start and end times for the current video");
            return false;
        }
    }

    private void showStats() {
        String local = SponsorBlock.storedLong("sb_local_skip_count") + " segments skipped\n"
                + DateUtils.formatElapsedTime(SponsorBlock.storedLong("sb_local_time_saved") / 1000) + " saved on this device";
        new AlertDialog.Builder(getContext()).setTitle("SponsorBlock statistics").setMessage(local)
                .setPositiveButton("OK", null).setNeutralButton("My contributions", (dialog, which) ->
                    SponsorBlockApi.execute(() -> {
                        try {
                            JSONObject info = new JSONObject(SponsorBlockApi.request("GET", "/api/userInfo",
                                    "userID", SponsorBlockApi.userId(),
                                    "values", "[\"userName\",\"segmentCount\",\"viewCount\",\"minutesSaved\"]"));
                            String message = info.optString("userName") + "\n" + info.optInt("segmentCount")
                                    + " submitted segments\n" + info.optInt("viewCount") + " skips by viewers\n"
                                    + info.optDouble("minutesSaved") + " minutes saved";
                            post(() -> {
                                if (((Activity) getContext()).isDestroyed()) return;
                                new AlertDialog.Builder(getContext()).setTitle("My contributions")
                                        .setMessage(message).setPositiveButton("OK", null).show();
                            });
                        } catch (Exception error) { SponsorBlockApi.toast("Could not load contribution statistics"); }
                    })).show();
    }
}
