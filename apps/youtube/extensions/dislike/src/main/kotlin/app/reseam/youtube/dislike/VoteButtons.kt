// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.dislike

import android.text.SpannableString
import android.text.Spanned
import app.reseam.bundle.Logger
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.ShortsSettings
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings

internal enum class NativeVoteButton(
    /** The first anchor in the path marks the button's label slot. */
    val anchors: List<String>,
    val text: () -> String,
    val unavailable: () -> String,
    val description: (String) -> String,
    val count: (ReturnYouTubeDislike) -> String?,
) {
    LIKE(
        listOf("|fullscreen_video_action_button.e", "|button_inner.e"),
        { YouTubeStrings.rydLikes },
        { YouTubeStrings.rydLikesUnavailable },
        { YouTubeStrings.rydLikesCount(it) },
        { it.nativeLikeCountText },
    ),
    DISLIKE(
        listOf("|button_inner.e"),
        { YouTubeStrings.rydDislikes },
        { YouTubeStrings.rydDislikesUnavailable },
        { YouTubeStrings.rydDislikesCount(it) },
        { it.dislikeCountText },
    ),
}

internal fun nativeVoteButton(path: CharSequence): NativeVoteButton? {
    val button = if ("|like_button.e" in path) NativeVoteButton.LIKE else NativeVoteButton.DISLIKE
    val index = button.anchors.firstNotNullOfOrNull { anchor -> path.indexOf(anchor).takeIf { it >= 0 } } ?: return null
    if (button == NativeVoteButton.DISLIKE && "|dislike_button_vm.e" !in path) return null
    val containerMarker = "|ContainerType|"
    return button.takeIf {
        path.indexOf(containerMarker, index) == path.length - containerMarker.length
    }
}

internal enum class ShortsButton(
    val paths: List<String>,
    val replacesVisibleCount: Boolean,
    val hidden: () -> Boolean,
    val replaceSpan: (ReturnYouTubeDislike, Spanned) -> Spanned,
) {
    DISLIKE(
        listOf("|shorts_dislike_button.e", "|reel_dislike_button.e"),
        true,
        { ShortsSettings.hideShortsDislikeButton.value },
        ReturnYouTubeDislike::getDislikeSpanForShort,
    ),
    LIKE(
        listOf("|shorts_like_button.e", "|reel_like_button.e"),
        false,
        { ShortsSettings.hideShortsLikeButton.value },
        ReturnYouTubeDislike::getLikeSpanForShort,
    );

    fun replace(original: CharSequence): CharSequence {
        if (!VideoSettings.rydShorts.value || hidden()) return original
        val data =
            if (decrementUseLithoDataIfNeeded()) {
                lastLithoShortsVideoData
            } else {
                currentVideoData
            }
        if (data == null) {
            Logger.debug { "Cannot modify Shorts litho span, data is null" }
            return original
        }
        return replaceSpan(data, original as? Spanned ?: SpannableString(original))
    }
}
