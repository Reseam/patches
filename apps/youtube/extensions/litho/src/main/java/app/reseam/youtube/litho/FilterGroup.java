// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/**
 * One or more patterns that hide a component, gated on a single setting.
 * <p>
 * Only selected patches register their filters. Each group reads its own setting and default.
 */
public abstract class FilterGroup<T> {

    public static final class FilterGroupResult {
        private int matchedIndex;
        private int matchedLength;

        FilterGroupResult() {
            this(-1, 0);
        }

        FilterGroupResult(int matchedIndex, int matchedLength) {
            setValues(matchedIndex, matchedLength);
        }

        public void setValues(int matchedIndex, int matchedLength) {
            this.matchedIndex = matchedIndex;
            this.matchedLength = matchedLength;
        }

        public boolean isFiltered() {
            return matchedIndex >= 0;
        }

        /** Matched index of the first pattern that matched, or -1 if nothing matched. */
        public int getMatchedIndex() {
            return matchedIndex;
        }

        public int getMatchedLength() {
            return matchedLength;
        }
    }

    /** Settings key this group is gated on, or null when the group is always on. */
    private final String setting;
    private final boolean settingDefault;
    public final T[] filters;

    @SafeVarargs
    public FilterGroup(String setting, boolean settingDefault, T... filters) {
        this.setting = setting;
        this.settingDefault = settingDefault;
        this.filters = filters;
        if (filters.length == 0) {
            throw new IllegalArgumentException("Must use one or more filter patterns (zero specified)");
        }
    }

    public boolean isEnabled() {
        return setting == null || Settings.getBoolean(setting, settingDefault);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + (setting == null ? "(always on)" : setting);
    }

    public abstract FilterGroupResult check(T stack);

    public static class StringFilterGroup extends FilterGroup<String> {

        public StringFilterGroup(String setting, boolean settingDefault, String... filters) {
            super(setting, settingDefault, filters);
        }

        @Override
        public FilterGroupResult check(String string) {
            int matchedIndex = -1;
            int matchedLength = 0;
            if (string != null && isEnabled() && !string.isEmpty()) {
                for (String pattern : filters) {
                    final int indexOf = string.indexOf(pattern);
                    if (indexOf >= 0) {
                        matchedIndex = indexOf;
                        matchedLength = pattern.length();
                        break;
                    }
                }
            }
            return new FilterGroupResult(matchedIndex, matchedLength);
        }
    }

    /**
     * For more than one pattern use {@link FilterGroupList.ByteArrayFilterGroupList#check(byte[])},
     * whose prefix tree beats repeated KMP searches.
     */
    public static class ByteArrayFilterGroup extends FilterGroup<byte[]> {

        private volatile int[][] failurePatterns;

        // Modified implementation from https://stackoverflow.com/a/1507813
        private static int indexOf(byte[] data, byte[] pattern, int[] failure) {
            int patternLength = pattern.length;
            if (patternLength == 0) return -1;
            for (int i = 0, j = 0, dataLength = data.length; i < dataLength; i++) {
                while (j > 0 && pattern[j] != data[i]) {
                    j = failure[j - 1];
                }
                if (pattern[j] == data[i]) {
                    j++;
                }
                if (j == patternLength) {
                    return i - patternLength + 1;
                }
            }
            return -1;
        }

        /** Computes the KMP failure function by matching the pattern against itself. */
        private static int[] createFailurePattern(byte[] pattern) {
            final int patternLength = pattern.length;
            final int[] failure = new int[patternLength];

            for (int i = 1, j = 0; i < patternLength; i++) {
                while (j > 0 && pattern[j] != pattern[i]) {
                    j = failure[j - 1];
                }
                if (pattern[j] == pattern[i]) {
                    j++;
                }
                failure[i] = j;
            }
            return failure;
        }

        public ByteArrayFilterGroup(String setting, boolean settingDefault, byte[]... filters) {
            super(setting, settingDefault, filters);
        }

        /** Converts the Strings into byte arrays, to search for text in binary data. */
        public ByteArrayFilterGroup(String setting, boolean settingDefault, String... filters) {
            super(setting, settingDefault, ByteTrieSearch.convertStringsToBytes(filters));
        }

        private synchronized void buildFailurePatterns() {
            if (failurePatterns != null) return; // Another thread won the race.
            Logger.debug(() -> "Building failure array for: " + this);
            int[][] patterns = new int[filters.length][];
            int i = 0;
            for (byte[] pattern : filters) {
                patterns[i++] = createFailurePattern(pattern);
            }
            this.failurePatterns = patterns; // Must be set only after initialization finishes.
        }

        @Override
        public FilterGroupResult check(byte[] bytes) {
            int matchedLength = 0;
            int matchedIndex = -1;
            if (bytes != null && isEnabled()) {
                int[][] failures = failurePatterns;
                if (failures == null) {
                    buildFailurePatterns();
                    failures = failurePatterns;
                }
                for (int i = 0, length = filters.length; i < length; i++) {
                    byte[] filter = filters[i];
                    matchedIndex = indexOf(bytes, filter, failures[i]);
                    if (matchedIndex >= 0) {
                        matchedLength = filter.length;
                        break;
                    }
                }
            }
            return new FilterGroupResult(matchedIndex, matchedLength);
        }
    }
}
