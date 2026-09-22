// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.video;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.player.Event;
import app.reseam.youtube.player.ShortsPlayerState;

/** Runtime state and injection points for the currently playing YouTube video. */
public final class VideoInformation {
    public interface PlaybackController {
        boolean patch_seekTo(long videoTime);
        void patch_seekToRelative(long videoTimeOffset);
    }

    public interface VideoQualityMenuInterface {
        void patch_setQuality(VideoQualityInterface quality);
    }

    public interface VideoQualityInterface {
        String patch_getQualityName();
        int patch_getResolution();
    }

    public static final int AUTOMATIC_VIDEO_QUALITY_VALUE = -2;

    private static final float DEFAULT_YOUTUBE_PLAYBACK_SPEED = 1.0f;
    private static final String SHORTS_PLAYER_PARAMETERS = "8AEB";

    private static WeakReference<PlaybackController> playerControllerRef = new WeakReference<>(null);
    private static WeakReference<PlaybackController> mdxPlayerDirectorRef = new WeakReference<>(null);

    private static String videoId = "";
    private static long videoLength;
    private static volatile long videoTime = -1;
    private static volatile String playerResponseVideoId = "";
    private static volatile boolean playerResponseVideoIdIsShort;
    private static volatile boolean videoIdIsShort;
    private static volatile float playbackSpeed = DEFAULT_YOUTUBE_PLAYBACK_SPEED;

    private static int desiredVideoResolution = AUTOMATIC_VIDEO_QUALITY_VALUE;
    private static boolean qualityNeedsUpdating;
    private static VideoQualityInterface[] currentQualities;
    private static VideoQualityInterface currentQuality;
    private static VideoQualityMenuInterface currentMenuInterface;

    public static final Event<VideoQualityInterface> onQualityChange = new Event<>();

    private VideoInformation() {}

    public static void initialize(PlaybackController playerController) {
        try {
            playerControllerRef = new WeakReference<>(playerController);
            videoTime = -1;
            videoLength = 0;
            playbackSpeed = DEFAULT_YOUTUBE_PLAYBACK_SPEED;
            desiredVideoResolution = AUTOMATIC_VIDEO_QUALITY_VALUE;
            qualityNeedsUpdating = false;
            currentQualities = null;
            currentMenuInterface = null;
            setCurrentQuality(null);
            Logger.debug(() -> "Video information initialized");
        } catch (Exception exception) {
            Logger.error(() -> "Video information initialize failure: " + exception);
        }
    }

    public static void initializeMDX(PlaybackController mdxPlayerDirector) {
        try {
            mdxPlayerDirectorRef = new WeakReference<>(mdxPlayerDirector);
            Logger.debug(() -> "MDX video information initialized");
        } catch (Exception exception) {
            Logger.error(() -> "MDX initialize failure: " + exception);
        }
    }

    public static void setVideoId(String newlyLoadedVideoId) {
        if (newlyLoadedVideoId == null) return;
        if (!videoId.equals(newlyLoadedVideoId)) {
            videoId = newlyLoadedVideoId;
            Logger.debug(() -> "Video ID: " + newlyLoadedVideoId);
        }
    }

    public static boolean playerParametersAreShort(String parameters) {
        return parameters != null && parameters.startsWith(SHORTS_PLAYER_PARAMETERS);
    }

    public static String newPlayerResponseSignature(
            String signature,
            String responseVideoId,
            boolean isShortAndOpeningOrPlaying
    ) {
        final boolean isShort = playerParametersAreShort(signature);
        playerResponseVideoIdIsShort = isShort;
        if (!isShort || isShortAndOpeningOrPlaying) {
            if (videoIdIsShort != isShort) {
                videoIdIsShort = isShort;
                Logger.debug(() -> "Opened video is Short: " + isShort);
            }
        }
        return signature;
    }

    public static void setPlayerResponseVideoId(String responseVideoId, boolean isShortAndOpeningOrPlaying) {
        if (responseVideoId == null) return;
        if (!playerResponseVideoId.equals(responseVideoId)) {
            playerResponseVideoId = responseVideoId;
            Logger.debug(() -> "Player-response video ID: " + responseVideoId);
        }
    }

    public static void videoSpeedChanged(float currentVideoSpeed) {
        if (playbackSpeed != currentVideoSpeed) {
            playbackSpeed = currentVideoSpeed;
            Logger.debug(() -> "Video speed changed: " + currentVideoSpeed);
        }
    }

    public static void userSelectedPlaybackSpeed(float selectedSpeed) {
        playbackSpeed = selectedSpeed;
        Logger.debug(() -> "User selected playback speed: " + selectedSpeed);
    }

    public static void setVideoLength(long length) {
        if (videoLength != length) {
            videoLength = length;
            Logger.debug(() -> "Video duration: " + length);
        }
    }

    public static void setVideoWindow(long start, long end) {
        setVideoLength(Math.max(0, end - start));
    }

    public static void setVideoTime(long currentPlaybackTime) {
        videoTime = currentPlaybackTime;
        Logger.debug(() -> "Video time: " + currentPlaybackTime);
    }

    public static boolean seekTo(long seekTime) {
        try {
            final long adjustedSeekTime = Math.max(0, videoLength > 0
                    ? Math.min(seekTime, videoLength - 250) : seekTime);
            if (videoTime <= seekTime && videoTime >= adjustedSeekTime) {
                Logger.debug(() -> "Ignoring seek near video end: " + seekTime);
                return false;
            }

            Logger.debug(() -> "Seeking to: " + adjustedSeekTime);
            PlaybackController controller = playerControllerRef.get();
            if (controller != null && controller.patch_seekTo(adjustedSeekTime)) return true;

            if (adjustedSeekTime / 1000 == videoTime / 1000) {
                Logger.debug(() -> "Skipping MDX seek within the same second");
                return false;
            }
            controller = mdxPlayerDirectorRef.get();
            return controller != null && controller.patch_seekTo(adjustedSeekTime);
        } catch (Exception exception) {
            Logger.error(() -> "seekTo failure: " + exception);
            return false;
        }
    }

    public static void seekToRelative(long seekTimeOffset) {
        try {
            Logger.debug(() -> "Seeking relative to: " + seekTimeOffset);
            PlaybackController controller = playerControllerRef.get();
            if (controller != null) controller.patch_seekToRelative(seekTimeOffset);

            final long adjustedOffset = seekTimeOffset < 0
                    ? Math.min(seekTimeOffset, -1000)
                    : Math.max(seekTimeOffset, 1000);
            controller = mdxPlayerDirectorRef.get();
            if (controller != null) controller.patch_seekToRelative(adjustedOffset);
        } catch (Exception exception) {
            Logger.error(() -> "seekToRelative failure: " + exception);
        }
    }

    public static String getVideoId() {
        return videoId;
    }

    public static String getPlayerResponseVideoId() {
        return playerResponseVideoId;
    }

    public static boolean lastPlayerResponseIsShort() {
        return playerResponseVideoIdIsShort;
    }

    public static boolean lastVideoIdIsShort() {
        return videoIdIsShort;
    }

    public static float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public static long getVideoLength() {
        return videoLength;
    }

    public static long getVideoTime() {
        return videoTime;
    }

    public static boolean isAtEndOfVideo() {
        return videoTime >= videoLength && videoLength > 0;
    }

    /** The patch replaces this body with the synthesized app-side speed override bridge. */
    public static void overridePlaybackSpeed(float speedOverride) {
        if (speedOverride > AUTOMATIC_VIDEO_QUALITY_VALUE) {
            Logger.debug(() -> "Overriding playback speed to: " + speedOverride);
        }
    }

    public static void setPlaybackSpeed(float newlyLoadedPlaybackSpeed) {
        videoSpeedChanged(newlyLoadedPlaybackSpeed);
    }

    public static void setDesiredVideoResolution(int resolution) {
        desiredVideoResolution = resolution;
        qualityNeedsUpdating = true;
        Logger.debug(() -> "Desired video resolution: " + resolution);
    }

    public static VideoQualityInterface[] getCurrentQualities() {
        return currentQualities;
    }

    public static VideoQualityInterface getCurrentQuality() {
        return currentQuality;
    }

    private static void setCurrentQuality(VideoQualityInterface quality) {
        if (currentQuality != quality) {
            currentQuality = quality;
            onQualityChange.fire(quality);
        }
    }

    public static void changeQuality(VideoQualityInterface quality) {
        if (currentMenuInterface == null) {
            Logger.error(() -> "Cannot change quality: menu interface is null");
            return;
        }
        currentMenuInterface.patch_setQuality(quality);
    }

    public static int fixVideoQualityResolution(String name, int quality) {
        try {
            if (name != null && !name.startsWith(Integer.toString(quality))) {
                int suffixIndex = name.indexOf('p');
                if (suffixIndex > 0) {
                    int fixedQuality = Integer.parseInt(name.substring(0, suffixIndex));
                    Logger.debug(() -> "Fixed quality resolution " + quality + " -> " + fixedQuality);
                    return fixedQuality;
                }
            }
        } catch (Exception exception) {
            Logger.error(() -> "fixVideoQualityResolution failure: " + exception);
        }
        return quality;
    }

    public static int setVideoQuality(
            VideoQualityInterface[] qualities,
            VideoQualityMenuInterface menu,
            int originalQualityIndex
    ) {
        try {
            if (qualities == null || qualities.length == 0) return originalQualityIndex;
            currentMenuInterface = menu;
            boolean availableQualitiesChanged = currentQualities == null || !Arrays.equals(currentQualities, qualities);
            if (availableQualitiesChanged) {
                currentQualities = qualities;
                Logger.debug(() -> "Video qualities: " + Arrays.toString(qualities));
            }

            originalQualityIndex = Math.max(0, Math.min(originalQualityIndex, qualities.length - 1));
            VideoQualityInterface updatedCurrentQuality = qualities[originalQualityIndex];
            if (updatedCurrentQuality != null &&
                    updatedCurrentQuality.patch_getResolution() != AUTOMATIC_VIDEO_QUALITY_VALUE &&
                    currentQuality != updatedCurrentQuality) {
                setCurrentQuality(updatedCurrentQuality);
            }

            if (desiredVideoResolution == AUTOMATIC_VIDEO_QUALITY_VALUE) return originalQualityIndex;
            if (qualityNeedsUpdating) qualityNeedsUpdating = false;
            else if (!availableQualitiesChanged) return originalQualityIndex;

            int lastQualityIndex = qualities.length - 1;
            for (int index = 0; index < qualities.length; index++) {
                VideoQualityInterface quality = qualities[index];
                if (quality == null) continue;
                int resolution = quality.patch_getResolution();
                if ((resolution != AUTOMATIC_VIDEO_QUALITY_VALUE && resolution <= desiredVideoResolution)
                        || index == lastQualityIndex) {
                    boolean qualityNeedsChange = index != originalQualityIndex;
                    if (qualityNeedsChange || !ShortsPlayerState.isOpen()) changeQuality(quality);
                    return qualityNeedsChange || !ShortsPlayerState.isOpen() ? index : originalQualityIndex;
                }
            }
        } catch (Exception exception) {
            Logger.error(() -> "setVideoQuality failure: " + exception);
        }
        return originalQualityIndex;
    }

    public static boolean isPremiumVideoQuality(VideoQualityInterface quality) {
        String name = quality == null ? null : quality.patch_getQualityName();
        return name != null && name.contains("Premium");
    }
}
