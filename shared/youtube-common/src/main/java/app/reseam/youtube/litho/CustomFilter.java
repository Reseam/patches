// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;

/**
 * Hides components matching expressions the user wrote, one per line, in the form
 * {@code ^path#accessibility$buffer} where everything but the path is optional.
 */
public final class CustomFilter extends Filter {

    /** Declared by the "Hide layout components" patch; absent until the user selects it. */
    private static final String SETTING = "custom_filter";
    private static final String EXPRESSIONS_SETTING = "custom_filter_strings";

    private static final class CustomFilterGroup extends StringFilterGroup {
        /** Requires the path to match from its start. Must be the first character. */
        private static final String SYNTAX_STARTS_WITH = "^";
        /** Separates the path from an accessibility pattern. */
        private static final String SYNTAX_ACCESSIBILITY_SYMBOL = "#";
        /** Separates the path or accessibility from a protocol buffer pattern. */
        private static final String SYNTAX_BUFFER_SYMBOL = "$";

        private static final Pattern SYNTAX = Pattern.compile(
                "(" // Map key group.
                        + "(\\Q" + SYNTAX_STARTS_WITH + "\\E?)"
                        + "([^\\Q" + SYNTAX_ACCESSIBILITY_SYMBOL + SYNTAX_BUFFER_SYMBOL + "\\E]*)"
                        + "(?:\\Q" + SYNTAX_ACCESSIBILITY_SYMBOL + "\\E([^\\Q" + SYNTAX_BUFFER_SYMBOL + "\\E]*))?"
                        + "(?:\\Q" + SYNTAX_BUFFER_SYMBOL + "\\E(.*))?"
                        + ")");

        static Collection<CustomFilterGroup> parse() {
            String expressions = Settings.getString(EXPRESSIONS_SETTING, "");
            if (expressions.isBlank()) {
                return Collections.emptyList();
            }

            // Keyed on the path with its optional ^ and accessibility pattern but without the
            // buffer pattern, so several expressions over one path share a group and the buffer
            // is searched exactly once. Unconstrained path rules stay separate.
            Map<String, CustomFilterGroup> result = new HashMap<>();

            for (String expression : expressions.split("\n")) {
                if (expression.isBlank()) continue;

                Matcher matcher = SYNTAX.matcher(expression);
                if (!matcher.find()) {
                    Logger.error(() -> "Invalid custom filter: " + expression);
                    continue;
                }

                final String mapKey = matcher.group(2) + matcher.group(3)
                        + (matcher.group(4) == null ? "" : "#" + matcher.group(4))
                        + (matcher.group(5) == null ? "" : "$");
                final boolean pathStartsWith = !matcher.group(2).isEmpty();
                final String path = matcher.group(3);
                final String accessibility = matcher.group(4);
                final String buffer = matcher.group(5);

                if (path.isBlank()
                        || (accessibility != null && accessibility.isEmpty())
                        || (buffer != null && buffer.isEmpty())) {
                    Logger.error(() -> "Invalid custom filter: " + expression);
                    continue;
                }

                CustomFilterGroup group = result.computeIfAbsent(mapKey, key -> new CustomFilterGroup(pathStartsWith, path));

                if (accessibility != null) {
                    group.addAccessibilityString(accessibility);
                }
                if (buffer != null) {
                    group.addBufferString(buffer);
                }
            }

            return result.values();
        }

        final boolean startsWith;
        StringTrieSearch accessibilitySearch;
        ByteTrieSearch bufferSearch;

        CustomFilterGroup(boolean startsWith, String path) {
            super(SETTING, false, path);
            this.startsWith = startsWith;
        }

        void addAccessibilityString(String accessibilityString) {
            if (accessibilitySearch == null) {
                accessibilitySearch = new StringTrieSearch();
            }
            accessibilitySearch.addPattern(accessibilityString);
        }

        void addBufferString(String bufferString) {
            if (bufferSearch == null) {
                bufferSearch = new ByteTrieSearch();
            }
            bufferSearch.addPattern(bufferString.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("CustomFilterGroup{path=");
            if (startsWith) builder.append(SYNTAX_STARTS_WITH);
            builder.append(filters[0]);
            if (accessibilitySearch != null) {
                builder.append(", accessibility=");
                builder.append(accessibilitySearch.getPatterns());
            }
            if (bufferSearch != null) {
                builder.append(", buffer=");
                for (byte[] bufferString : bufferSearch.getPatterns()) {
                    builder.append(new String(bufferString, StandardCharsets.UTF_8));
                    builder.append('|');
                }
            }
            builder.append('}');
            return builder.toString();
        }
    }

    public CustomFilter() {
        Collection<CustomFilterGroup> groups = CustomFilterGroup.parse();

        if (!groups.isEmpty()) {
            CustomFilterGroup[] array = groups.toArray(new CustomFilterGroup[0]);
            Logger.debug(() -> "Using custom filters: " + Arrays.toString(array));
            addPathCallbacks(array);
        }
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        CustomFilterGroup custom = (CustomFilterGroup) matchedGroup;

        if (custom.startsWith && contentIndex != 0) {
            return false;
        }
        if (custom.accessibilitySearch != null && !custom.accessibilitySearch.matches(accessibility)) {
            return false;
        }
        return custom.bufferSearch == null || custom.bufferSearch.matches(buffer);
    }
}
