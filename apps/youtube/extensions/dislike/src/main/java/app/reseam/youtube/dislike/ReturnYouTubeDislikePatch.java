// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.dislike;

import static app.reseam.youtube.dislike.ReturnYouTubeDislike.Vote;

import android.graphics.drawable.ShapeDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;


import java.util.Objects;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.video.VideoInformation;
import app.reseam.youtube.dislike.RuntimeUtils;
import app.reseam.youtube.dislike.ReturnYouTubeDislikeFilter;
import app.reseam.youtube.dislike.ReturnYouTubeDislike;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.player.PlayerType;

/**
 * Handles all interaction of UI patch components.
 *
 * Fetches are asynchronous. A bind that precedes the response retains the original text;
 * subsequent binds use cached counts without blocking playback or Litho layout threads.
 */
@SuppressWarnings("unused")
public class ReturnYouTubeDislikePatch {

    /**
     * RYD data for the current video on screen.
     */

    private static volatile ReturnYouTubeDislike currentVideoData;

    /**
     * The last litho based Shorts loaded.
     * Maybe the same value as {@link #currentVideoData}, but usually is the next short to swipe to.
     */

    private static volatile ReturnYouTubeDislike lastLithoShortsVideoData;

    /**
     * Because litho Shorts spans are created offscreen after {@link ReturnYouTubeDislikeFilter}
     * detects the video IDs, but the current Short can arbitrarily reload the same span,
     * then use the {@link #lastLithoShortsVideoData} if this value is greater than zero.
     */
    private static int useLithoShortsVideoDataCount;

    /**
     * Last video ID prefetched. Field is to prevent prefetching the same video ID multiple times in a row.
     */

    private static volatile String lastPrefetchedVideoId;

    private static void clearData() {
        currentVideoData = null;
        lastLithoShortsVideoData = null;
        synchronized (ReturnYouTubeDislikePatch.class) {
            useLithoShortsVideoDataCount = 0;
        }

        // Rolling number text should not be cleared,
        // as it's used if incognito Short is opened/closed
        // while a regular video is on screen.
    }

    /**
     * @return If {@link #useLithoShortsVideoDataCount} was greater than zero.
     */
    private static boolean decrementUseLithoDataIfNeeded() {
        synchronized (ReturnYouTubeDislikePatch.class) {
            if (useLithoShortsVideoDataCount > 0) {
                useLithoShortsVideoDataCount--;
                return true;
            }

            return false;
        }
    }

    //
    // Litho player for both regular videos and Shorts.
    //

    /**
     * Injection point.
     *
     * Logs if new litho text layout is used.
     */
    public static boolean useNewLithoTextCreation(boolean useNewLithoTextCreation) {
        // Don't force flag on/off unless debugging patch hooks,
        // because forcing off with newer YT targets causes Shorts player to show no buttons,
        // presumably because the old litho data isn't in the layout data.
        Logger.debug(() -> "useNewLithoTextCreation: " + useNewLithoTextCreation);
        return useNewLithoTextCreation;
    }

    /**
     * Injection point.
     *
     * For Litho segmented buttons and Litho Shorts player.
     */

    public static CharSequence onLithoTextLoaded( StringBuilder path,
                                                  CharSequence original) {
        return onLithoTextLoaded(path, original, false);
    }

    /**
     * Called when a litho text component is initially created,
     * and also when a Span is later reused again (such as scrolling off/on screen).
     *
     * This method is sometimes called on the main thread, but it is usually called _off_ the main thread.
     * This method can be called multiple times for the same UI element (including after dislikes was added).
     *
     * @param original Original char sequence was created or reused by Litho.
     * @param isRollingNumber If the span is for a Rolling Number.
     * @return The original char sequence (if nothing should change), or a replacement char sequence that contains dislikes.
     */

    private static CharSequence onLithoTextLoaded( StringBuilder path,
                                                   CharSequence original,
                                                  boolean isRollingNumber) {
        try {
            if (path == null || !Settings.getBoolean("ryd_enabled", true)) {
                return original;
            }

            if (isRollingNumber && path.indexOf("video_action_bar.e") < 0) {
                return original;
            }

            if (path.indexOf("segmented_like_dislike_button.e") >= 0) {
                // Regular video.
                ReturnYouTubeDislike videoData = currentVideoData;
                if (videoData == null) {
                    return original; // User enabled RYD while a video was on screen.
                }
                if (!(original instanceof Spanned)) {
                    original = new SpannableString(original);
                }
                return videoData.getDislikesSpanForRegularVideo((Spanned) original,
                        true, isRollingNumber);
            }

            if (isRollingNumber) {
                return original; // No need to check for Shorts in the context.
            }

            if (path.indexOf("|shorts_dislike_button.e") >= 0
                    || path.indexOf("|reel_dislike_button.e") >= 0) {
                return getShortsSpan(original, true);
            }

            if (path.indexOf("|shorts_like_button.e") >= 0
                    || path.indexOf("|reel_like_button.e") >= 0) {
                if (!RuntimeUtils.containsNumber(original)) {
                    Logger.debug(() -> "Replacing hidden likes count");
                    return getShortsSpan(original, false);
                } else {
                    decrementUseLithoDataIfNeeded();
                }
            }
        } catch (Exception ex) {
            Logger.error(() -> "onLithoTextLoaded failure", ex);
        }
        return original;
    }

    private static CharSequence getShortsSpan( CharSequence original, boolean isDislikesSpan) {
        // Litho Shorts player.
        if (!Settings.getBoolean("ryd_shorts", true) || (isDislikesSpan && Settings.getBoolean("hide_shorts_dislike_button", false))
                || (!isDislikesSpan && Settings.getBoolean("hide_shorts_like_button", false))) {
            return original;
        }

        final ReturnYouTubeDislike videoData;
        if (decrementUseLithoDataIfNeeded()) {
            // New Short is loading off-screen.
            videoData = lastLithoShortsVideoData;
        } else {
            videoData = currentVideoData;
        }

        if (videoData == null) {
            // The Shorts litho video ID filter did not detect the video ID.
            // This is normal in incognito mode, but otherwise is abnormal.
            Logger.debug(() -> "Cannot modify Shorts litho span, data is null");
            return original;
        }

        Spanned span = original instanceof Spanned ? (Spanned) original : new SpannableString(original);
        return isDislikesSpan
                ? videoData.getDislikeSpanForShort(span)
                : videoData.getLikeSpanForShort(span);
    }

    //
    // Rolling Number
    //

    /**
     * Current regular video rolling number text, if rolling number is in use.
     * This is saved to a field as it's used in every draw() call.
     */

    private static volatile CharSequence rollingNumberSpan;

    /**
     * Injection point.
     */
    public static String onRollingNumberLoaded( StringBuilder path,
                                                String original) {
        try {
            CharSequence replacement = onLithoTextLoaded(path, original, true);

            String replacementString = replacement.toString();
            if (!replacementString.equals(original)) {
                rollingNumberSpan = replacement;
                return replacementString;
            } // Else, the text was not a likes count but instead the view count or something else.
        } catch (Exception ex) {
            Logger.error(() -> "onRollingNumberLoaded failure", ex);
        }
        return original;
    }

    /**
     * Injection point.
     *
     * Called for all usage of Rolling Number.
     * Modifies the measured String text width to include the left separator and padding, if needed.
     */
    public static float onRollingNumberMeasured(String text, float measuredTextWidth) {
        try {
            if (Settings.getBoolean("ryd_enabled", true)) {
                if (ReturnYouTubeDislike.isPreviouslyCreatedSegmentedSpan(text)) {
                    // +1 pixel is needed for some foreign languages that measure
                    // the text different from what is used for layout (Greek in particular).
                    // Probably a bug in Android, but who knows.
                    // Single line mode is also used as an additional fix for this issue.
                    if (Settings.getBoolean("ryd_compact_layout", false)) {
                        return measuredTextWidth + 1;
                    }

                    return measuredTextWidth + 1
                            + ReturnYouTubeDislike.leftSeparatorBounds.right
                            + ReturnYouTubeDislike.leftSeparatorShapePaddingPixels;
                }
            }
        } catch (Exception ex) {
            Logger.error(() -> "onRollingNumberMeasured failure", ex);
        }

        return measuredTextWidth;
    }

    /**
     * Add Rolling Number text view modifications.
     */
    private static void addRollingNumberPatchChanges(TextView view) {
        // YouTube Rolling Numbers do not use compound drawables or drawable padding.
        if (view.getCompoundDrawablePadding() == 0) {
            Logger.debug(() -> "Adding rolling number TextView changes");
            view.setCompoundDrawablePadding(ReturnYouTubeDislike.leftSeparatorShapePaddingPixels);
            ShapeDrawable separator = ReturnYouTubeDislike.getLeftSeparatorDrawable();
            if (RuntimeUtils.isRightToLeftLocale()) {
                view.setCompoundDrawables(null, null, separator, null);
            } else {
                view.setCompoundDrawables(separator, null, null, null);
            }

            // Disliking can cause the span to grow in size, which is ok and is laid out correctly,
            // but if the user then removes their dislike the layout will not adjust to the new shorter width.
            // Use a center alignment to take up any extra space.
            view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            // Single line mode does not clip words if the span is larger than the view bounds.
            // The styled span applied to the view should always have the same bounds,
            // but use this feature just in case the measurements are somehow off by a few pixels.
            view.setSingleLine(true);
        }
    }

    /**
     * Remove Rolling Number text view modifications made by this patch.
     * Required as it appears text views can be reused for other rolling numbers (view count, upload time, etc.).
     */
    private static void removeRollingNumberPatchChanges(TextView view) {
        if (view.getCompoundDrawablePadding() != 0) {
            Logger.debug(() -> "Removing rolling number TextView changes");
            view.setCompoundDrawablePadding(0);
            view.setCompoundDrawables(null, null, null, null);
            view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY); // Default alignment
            view.setSingleLine(false);
        }
    }

    /**
     * Injection point.
     */
    public static CharSequence updateRollingNumber(TextView view, CharSequence original) {
        try {
            if (!Settings.getBoolean("ryd_enabled", true)) {
                removeRollingNumberPatchChanges(view);
                return original;
            }
            // Called for all instances of RollingNumber, so must check if text is for a dislikes.
            // Text will already have the correct content, but it's missing the drawable separators.
            if (!ReturnYouTubeDislike.isPreviouslyCreatedSegmentedSpan(original.toString())) {
                // The text is the video view count, upload time, or some other text.
                removeRollingNumberPatchChanges(view);
                return original;
            }

            CharSequence replacement = rollingNumberSpan;
            if (replacement == null) {
                // User enabled RYD while a video was open,
                // or user opened/closed a Short while a regular video was opened.
                Logger.debug(() -> "Cannot update rolling number (field is null");
                removeRollingNumberPatchChanges(view);
                return original;
            }

            if (Settings.getBoolean("ryd_compact_layout", false)) {
                removeRollingNumberPatchChanges(view);
            } else {
                addRollingNumberPatchChanges(view);
            }

            // Remove any padding set by Rolling Number.
            view.setPadding(0, 0, 0, 0);

            // When displaying dislikes, the rolling animation is not visually correct
            // and the dislikes always animate (even though the dislike count has not changed).
            // The animation is caused by an image span attached to the span,
            // and using only the modified segmented span prevents the animation from showing.
            return replacement;
        } catch (Exception ex) {
            Logger.error(() -> "updateRollingNumber failure", ex);
            return original;
        }
    }

    //
    // Video ID and voting hooks (all players).
    //

    /**
     * Injection point.  Uses 'playback response' video ID hook to preload RYD.
     */
    public static void preloadVideoId( String videoId, boolean isShortAndOpeningOrPlaying) {
        try {
            if (!Settings.getBoolean("ryd_enabled", true)) {
                return;
            }
            if (videoId.equals(lastPrefetchedVideoId)) {
                return;
            }
            if (!RuntimeUtils.isNetworkConnected()) {
                Logger.debug(() -> "Cannot pre-fetch RYD, network is not connected");
                lastPrefetchedVideoId = null;
                return;
            }

            final boolean videoIdIsShort = VideoInformation.lastPlayerResponseIsShort();
            // Shorts shelf in home and subscription feed causes player response hook to be called,
            // and the 'is opening/playing' parameter will be false.
            // This hook will be called again when the Short is actually opened.
            if (videoIdIsShort && (!isShortAndOpeningOrPlaying || !Settings.getBoolean("ryd_shorts", true))) {
                return;
            }
            Logger.debug(() -> "Prefetching RYD for video: " + videoId);
            ReturnYouTubeDislike.getFetchForVideoId(videoId);
            lastPrefetchedVideoId = videoId;
        } catch (Exception ex) {
            Logger.error(() -> "preloadVideoId failure", ex);
        }
    }

    /**
     * Injection point. Uses 'current playing' video ID hook. Always called on main thread.
     */
    public static void newVideoLoaded( String videoId) {
        try {
            DislikeLabel.newVideoLoaded(videoId);
            if (!Settings.getBoolean("ryd_enabled", true)) return;
            Objects.requireNonNull(videoId);

            PlayerType currentPlayerType = PlayerType.current();
            final boolean isNoneHiddenOrSlidingMinimized = currentPlayerType.isNoneHiddenOrSlidingMinimized();
            if (isNoneHiddenOrSlidingMinimized && !Settings.getBoolean("ryd_shorts", true)) {
                // Must clear here, otherwise the wrong data can be used for a minimized regular video.
                clearData();
                return;
            }

            if (videoIdIsSame(currentVideoData, videoId)) {
                return;
            }
            Logger.debug(() -> "New video ID: " + videoId + " playerType: " + currentPlayerType);

            if (!RuntimeUtils.isNetworkConnected()) {
                Logger.debug(() -> "Cannot fetch RYD, network is not connected");
                currentVideoData = null;
                return;
            }

            ReturnYouTubeDislike data = ReturnYouTubeDislike.getFetchForVideoId(videoId);
            // Pre-emptively set the data to short status.
            // Required to prevent Shorts data from being used on a minimized video in incognito mode.
            if (isNoneHiddenOrSlidingMinimized) {
                data.setVideoIdIsShort(true);
            }
            currentVideoData = data;
        } catch (Exception ex) {
            Logger.error(() -> "newVideoLoaded failure", ex);
        }
    }

    public static void setLastLithoShortsVideoId( String videoId) {
        if (videoIdIsSame(lastLithoShortsVideoData, videoId)) {
            return;
        }

        if (videoId == null) {
            // Litho filter did not detect the video ID.  App is in incognito mode,
            // or the proto buffer structure was changed and the video ID is no longer present.
            // Must clear both currently playing and last litho data otherwise the
            // next regular video may use the wrong data.
            Logger.debug(() -> "Litho filter did not find any video IDs");
            clearData();
            return;
        }

        Logger.debug(() -> "New litho Shorts video ID: " + videoId);
        ReturnYouTubeDislike videoData = ReturnYouTubeDislike.getFetchForVideoId(videoId);
        videoData.setVideoIdIsShort(true);
        lastLithoShortsVideoData = videoData;
        synchronized (ReturnYouTubeDislikePatch.class) {
            // Use litho Shorts data for the next like and dislike spans.
            useLithoShortsVideoDataCount = 2;
        }
    }

    private static boolean videoIdIsSame( ReturnYouTubeDislike fetch,  String videoId) {
        return (fetch == null && videoId == null)
                || (fetch != null && fetch.getVideoId().equals(videoId));
    }

    /**
     * Injection point.
     *
     * Called when the user likes or dislikes.
     *
     * @param vote int that matches {@link Vote#value}
     */
    public static void sendVote(int vote) {
        try {
            if (!Settings.getBoolean("ryd_enabled", true)) {
                return;
            }

            final boolean isNoneHiddenOrMinimized = PlayerType.current().isNoneHiddenOrMinimized();
            if (isNoneHiddenOrMinimized && !Settings.getBoolean("ryd_shorts", true)) {
                return;
            }

            ReturnYouTubeDislike videoData = currentVideoData;
            if (videoData == null) {
                Logger.debug(() -> "Cannot send vote, as current video data is null");
                return; // User enabled RYD while a regular video was minimized.
            }

            for (Vote v : Vote.values()) {
                if (v.value == vote) {
                    videoData.sendVote(v);
                    return;
                }
            }

            Logger.error(() -> "Unknown vote type: " + vote);
        } catch (Exception ex) {
            Logger.error(() -> "sendVote failure", ex);
        }
    }
}
