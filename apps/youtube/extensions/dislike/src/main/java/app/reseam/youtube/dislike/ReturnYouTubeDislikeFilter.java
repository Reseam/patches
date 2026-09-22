// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.dislike;


import java.util.LinkedHashSet;
import java.util.Map;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.dislike.RuntimeUtils;
import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.*;
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList;
import app.reseam.youtube.dislike.ReturnYouTubeDislikePatch;
import app.reseam.youtube.video.VideoInformation;
import app.reseam.youtube.core.Settings;

/**
 * Searches for video IDs in the proto buffer of Shorts dislike.
 *
 * Because multiple litho dislike spans are created in the background
 * (and also anytime litho refreshes the components, which is somewhat arbitrary),
 * that makes the value of {@link VideoInformation#getVideoId()} and {@link VideoInformation#getPlayerResponseVideoId()}
 * unreliable to determine which video ID a Shorts litho span belongs to.
 *
 * But the correct video ID does appear in the protobuffer just before a Shorts litho span is created.
 *
 * Once a way to asynchronously update litho text is found, this strategy will no longer be needed.
 */
public final class ReturnYouTubeDislikeFilter extends Filter {

    /**
     * Last unique video IDs loaded. Value is ignored and Map is treated as a Set.
     * Cannot use {@link LinkedHashSet} because it's missing #removeEldestEntry().
     */
    private static final Map<String, Boolean> lastVideoIds = RuntimeUtils.createSizeRestrictedMap(5);

    /**
     * Injection point.
     */
    @SuppressWarnings("unused")
    public static void newPlayerResponseVideoId(String videoId, boolean isShortAndOpeningOrPlaying) {
        try {
            if (!isShortAndOpeningOrPlaying || !Settings.getBoolean("ryd_enabled", true) || !Settings.getBoolean("ryd_shorts", true)) {
                return;
            }
            synchronized (lastVideoIds) {
                if (lastVideoIds.put(videoId, Boolean.TRUE) == null) {
                    Logger.debug(() -> "New Short video ID: " + videoId);
                }
            }
        } catch (Exception ex) {
            Logger.error(() -> "newPlayerResponseVideoId failure", ex);
        }
    }

    private final ByteArrayFilterGroupList videoIdFilterGroup = new ByteArrayFilterGroupList();

    public ReturnYouTubeDislikeFilter() {
        // When a new Short is opened, the like buttons always seem to load before the dislike.
        // But if swiping back to a previous video and liking/disliking, then only that single button reloads.
        // So must check for both buttons.
        addPathCallbacks(
                new StringFilterGroup(
                        null, true,
                        "shorts_like_button.e",
                        "reel_like_button.e",
                        "reel_like_toggled_button.e",
                        "shorts_dislike_button.e",
                        "reel_dislike_button.e",
                        "reel_dislike_toggled_button.e"
                )
        );

        // After the button identifiers is binary data and then the video ID for that specific short.
        videoIdFilterGroup.addAll(
                new ByteArrayFilterGroup(
                        null, true,
                        "id.reel_like_button",
                        "id.reel_dislike_button",
                        "ic_right_like",
                        "ic_right_dislike"
                )
        );
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (!Settings.getBoolean("ryd_enabled", true) || !Settings.getBoolean("ryd_shorts", true)) {
            return false;
        }

        FilterGroupResult result = videoIdFilterGroup.check(buffer);
        if (result.isFiltered()) {
            String matchedVideoId = findVideoId(buffer);
            // Matched video will be null if in incognito mode.
            // Must pass a null ID to correctly clear out the current video data.
            // Otherwise, if a Short is opened in non-incognito, then incognito is enabled and another Short is opened,
            // the new incognito Short will show the old prior data.
            ReturnYouTubeDislikePatch.setLastLithoShortsVideoId(matchedVideoId);
        }

        return false;
    }


    private String findVideoId(byte[] protobufBufferArray) {
        synchronized (lastVideoIds) {
            for (String videoId : lastVideoIds.keySet()) {
                if (byteArrayContainsString(protobufBufferArray, videoId)) {
                    return videoId;
                }
            }

            return null;
        }
    }

    /**
     * This could use a trie, but since the patterns are constantly changing
     * the overhead of updating the Trie might negate the search performance gain.
     */
    private static boolean byteArrayContainsString( byte[] array,  String text) {
        for (int i = 0, lastArrayStartIndex = array.length - text.length(); i <= lastArrayStartIndex; i++) {
            boolean found = true;
            for (int j = 0, textLength = text.length(); j < textLength; j++) {
                if (array[i + j] != (byte) text.charAt(j)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }

        return false;
    }
}
