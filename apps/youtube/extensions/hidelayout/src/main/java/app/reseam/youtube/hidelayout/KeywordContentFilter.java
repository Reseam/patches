// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout;

import android.os.SystemClock;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.litho.ByteTrieSearch;
import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.litho.StringTrieSearch;
import app.reseam.youtube.navigation.NavigationBar;
import app.reseam.youtube.player.PlayerType;

/** Hides home, subscription and search results whose protocol buffer contains a keyword. */
public final class KeywordContentFilter extends Filter {
    private static final String[] COMMON_BUFFER_TEXT = {
            "googlevideo.com/initplayback?source=youtube", "ANDROID", "https://i.ytimg.com/vi/",
            "mqdefault.jpg", "hqdefault.jpg", "sddefault.jpg", "hq720.jpg", "webp", "_custom_",
            "OMX.google.vp9.decoder", "OMX.google.av1.decoder", "OMX.sprd.av1.decoder",
            "c2.android.av1.decoder", "c2.android.vp9.decoder", "searchR", "browse-feed",
            "FEwhat_to_watch", "FEsubscriptions", "search_vwc_description_transition_key",
            "g-high-recZ", "expandable_metadata.e", "thumbnail.e", "avatar.e", "overflow_button.e",
            "shorts-lockup-image", "shorts-lockup.overlay-metadata.secondary-text", "YouTubeSans-SemiBold",
            "sans-serif"
    };
    private static final int MINIMUM_KEYWORD_LENGTH = 3;
    private static final float ALL_VIDEOS_FILTERED_THRESHOLD = .95f;
    private static final float ALL_VIDEOS_FILTERED_SAMPLE_SIZE = 50f;
    private static final long ALL_VIDEOS_FILTERED_BACKOFF_MILLISECONDS = 60_000L;
    private static final int UTF8_MAX_BYTE_COUNT = 4;
    private static final StringFilterGroup STARTS_WITH = new StringFilterGroup(null, false,
            "home_video_with_context.e", "search_video_with_context.e", "video_with_context.e",
            "related_video_with_context.e", "video_lockup_with_attachment.e", "compact_video.e",
            "inline_shorts", "shorts_video_cell", "shorts_pivot_item.e");
    private static final StringFilterGroup CONTAINS = new StringFilterGroup(null, false,
            "modern_type_shelf_header_content.e", "shorts_lockup_cell.e", "video_card.e");
    private final StringTrieSearch exceptions = new StringTrieSearch();
    private volatile String parsedText;
    private volatile ByteTrieSearch search = new ByteTrieSearch();
    private volatile long resumeAt;
    private volatile float filteredPercentage;

    public KeywordContentFilter() {
        for (String value : new String[]{"metadata.e", "thumbnail.e", "avatar.e", "overflow_button.e"}) {
            exceptions.addPattern(value);
        }
        addPathCallbacks(STARTS_WITH, CONTAINS);
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == STARTS_WITH && contentIndex != 0) return false;
        if (!isEnabledForCurrentSurface() || exceptions.matches(path)) return false;
        parseIfNeeded();
        AtomicReference<String> matched = new AtomicReference<>();
        if (search.matches(buffer, matched)) {
            Logger.debug(() -> "KeywordContentFilter filtered path: " + path + " keyword=" + matched.get());
            updateBackoff(true);
            return true;
        }
        updateBackoff(false);
        return false;
    }

    private boolean isEnabledForCurrentSurface() {
        if (resumeAt != 0) {
            if (SystemClock.elapsedRealtime() < resumeAt) return false;
            resumeAt = 0;
            filteredPercentage = 0;
            Logger.debug(() -> "KeywordContentFilter resumed after broad-filter pause");
        }
        if (PlayerType.current().isMaximizedOrFullscreen()) {
            return Settings.getBoolean("hide_keyword_content_home", false);
        }
        if (NavigationBar.isSearchBarActive()) return Settings.getBoolean("hide_keyword_content_search", false);
        boolean hideHome = Settings.getBoolean("hide_keyword_content_home", false);
        boolean hideSubscriptions = Settings.getBoolean("hide_keyword_content_subscriptions", false);
        if (!hideHome && !hideSubscriptions) return false;
        NavigationBar.NavigationButton selected = NavigationBar.NavigationButton.getSelectedNavigationButton();
        if (selected == null) return hideHome;
        switch (selected) {
            case HOME:
            case EXPLORE:
                return hideHome;
            case SUBSCRIPTIONS:
                return hideSubscriptions;
            default:
                return false;
        }
    }

    private synchronized void parseIfNeeded() {
        String raw = Settings.getString("hide_keyword_content_phrases", "");
        if (raw.equals(parsedText)) return;
        Map<String, Boolean> keywords = new LinkedHashMap<>();
        Set<String> conflicting = new HashSet<>();
        for (String line : raw.split("\\n")) {
            String phrase = line.stripTrailing();
            if (phrase.isEmpty()) continue;
            boolean wholeWord = phrase.length() >= 2 && phrase.startsWith("\"") && phrase.endsWith("\"");
            if (wholeWord) phrase = phrase.substring(1, phrase.length() - 1);
            if (phrase.isEmpty() || (!wholeWord && phrase.length() < MINIMUM_KEYWORD_LENGTH
                    && !isLanguageWithNoSpaces(phrase))) {
                Logger.debug(() -> "KeywordContentFilter ignored a short phrase");
                continue;
            }
            String[] variants = {phrase, phrase.toLowerCase(Locale.ROOT), titleCase(phrase), capitalizeWords(phrase), phrase.toUpperCase(Locale.ROOT)};
            if (phrasesWillHideAllVideos(variants, wholeWord)) {
                Logger.debug(() -> "KeywordContentFilter ignored a phrase that matches every video");
                continue;
            }
            for (String variant : variants) {
                if (conflicting.contains(variant)) continue;
                Boolean previous = keywords.get(variant);
                if (previous == null) {
                    keywords.put(variant, wholeWord);
                } else if (previous != wholeWord) {
                    keywords.remove(variant);
                    conflicting.add(variant);
                    Logger.debug(() -> "KeywordContentFilter ignored conflicting phrase: " + variant);
                }
            }
        }

        ByteTrieSearch next = new ByteTrieSearch();
        for (Map.Entry<String, Boolean> entry : keywords.entrySet()) {
            final String keyword = entry.getKey();
            final boolean wholeWord = entry.getValue();
            next.addPattern(keyword.getBytes(StandardCharsets.UTF_8), (bytes, start, length, result) -> {
                if (wholeWord && !isWholeWord(bytes, start, length)) return false;
                ((AtomicReference<String>) result).set(keyword);
                return true;
            });
        }
        search = next;
        parsedText = raw;
        resumeAt = 0;
        filteredPercentage = 0;
        Logger.debug(() -> "KeywordContentFilter loaded " + keywords.size() + " phrases");
    }

    private synchronized void updateBackoff(boolean filtered) {
        float updated = filteredPercentage * ((ALL_VIDEOS_FILTERED_SAMPLE_SIZE - 1) / ALL_VIDEOS_FILTERED_SAMPLE_SIZE);
        if (filtered) updated += 1f / ALL_VIDEOS_FILTERED_SAMPLE_SIZE;
        if (updated > ALL_VIDEOS_FILTERED_THRESHOLD) {
            resumeAt = SystemClock.elapsedRealtime() + ALL_VIDEOS_FILTERED_BACKOFF_MILLISECONDS;
            filteredPercentage = 0;
            Logger.debug(() -> "KeywordContentFilter paused after filtering nearly every component");
        } else {
            filteredPercentage = updated;
        }
    }

    private static String titleCase(String value) {
        if (value.isEmpty()) return value;
        int first = value.codePointAt(0);
        return new StringBuilder().appendCodePoint(Character.toTitleCase(first))
                .append(value, Character.charCount(first), value.length()).toString();
    }

    private static String capitalizeWords(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean next = true;
        for (int i = 0; i < value.length();) {
            int codePoint = value.codePointAt(i);
            if (codePoint == ' ') next = true;
            else if (next) { codePoint = Character.toUpperCase(codePoint); next = false; }
            result.appendCodePoint(codePoint);
            i += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private static boolean isLanguageWithNoSpaces(String value) {
        for (int i = 0; i < value.length();) {
            int codePoint = value.codePointAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.HIRAGANA
                    || block == Character.UnicodeBlock.KATAKANA
                    || block == Character.UnicodeBlock.THAI
                    || block == Character.UnicodeBlock.LAO
                    || block == Character.UnicodeBlock.MYANMAR
                    || block == Character.UnicodeBlock.KHMER
                    || block == Character.UnicodeBlock.TIBETAN) return true;
            i += Character.charCount(codePoint);
        }
        return false;
    }

    private static boolean phrasesWillHideAllVideos(String[] phrases, boolean wholeWord) {
        for (String phrase : phrases) {
            byte[] phraseBytes = phrase.getBytes(StandardCharsets.UTF_8);
            for (String common : COMMON_BUFFER_TEXT) {
                int start = common.indexOf(phrase);
                while (start >= 0) {
                    if (!wholeWord || isWholeWord(common.getBytes(StandardCharsets.UTF_8), start, phraseBytes.length)) {
                        return true;
                    }
                    start = common.indexOf(phrase, start + 1);
                }
            }
        }
        return false;
    }

    private static boolean isWholeWord(byte[] data, int start, int length) {
        int before = utf8Before(data, start);
        int after = utf8At(data, start + length);
        return (before < 0 || !Character.isLetter(before)) && (after < 0 || !Character.isLetter(after));
    }

    private static int utf8Before(byte[] data, int index) {
        if (index <= 0) return -1;
        for (int count = 1; count <= UTF8_MAX_BYTE_COUNT && index - count >= 0; count++) {
            int start = index - count;
            if (utf8At(data, start) >= 0 && utf8ByteCount(data[start]) == count) return utf8At(data, start);
        }
        return -1;
    }

    private static int utf8At(byte[] data, int index) {
        if (index < 0 || index >= data.length) return -1;
        int first = data[index] & 0xFF;
        if ((first & 0x80) == 0) return first;
        int count = utf8ByteCount((byte) first);
        if (count == 0 || index + count > data.length) return -1;
        int value = first & ((1 << (7 - count)) - 1);
        for (int i = 1; i < count; i++) {
            int next = data[index + i] & 0xFF;
            if ((next & 0xC0) != 0x80) return -1;
            value = (value << 6) | (next & 0x3F);
        }
        return value;
    }

    private static int utf8ByteCount(byte first) {
        int value = first & 0xFF;
        return (value & 0x80) == 0 ? 1
                : (value & 0xE0) == 0xC0 ? 2
                : (value & 0xF0) == 0xE0 ? 3
                : (value & 0xF8) == 0xF0 ? 4 : 0;
    }
}
