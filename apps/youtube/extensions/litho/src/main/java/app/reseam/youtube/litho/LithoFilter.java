// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;

/**
 * Decides whether a litho component is hidden, from its identifier, its path and the protocol
 * buffer it was built from.
 * <p>
 * The buffer arrives from a different injection point than the identifier and path, and litho
 * builds components on several threads at once, so the two are joined by the identifier the
 * buffer itself carries rather than by call order.
 */
public final class LithoFilter {

    /**
     * The filters in play. Each patch that ships one registers it at process start, so only the
     * filters of selected patches are ever searched. This replaces rewriting an array at patch time.
     */
    private static final List<Filter> filters = new ArrayList<>();

    /**
     * Unpatched YouTube sizes the litho layout pool by core count and memory, between 1 and 3
     * threads. More than one causes shelves to be hidden that no filter asked to hide, and races
     * the navigation-tab state that several filters read.
     */
    private static final int LAYOUT_THREAD_POOL_SIZE = 1;

    /**
     * Component identifiers end in one of ".eml", ".eml-fe", ".e-b", ".eml-js" or "e-js-b", so
     * ".e" is the shortest prefix that finds all of them.
     */
    private static final byte[] COMPONENT_EXTENSION_BYTES = ".e".getBytes(StandardCharsets.US_ASCII);

    /** Stands in for the buffer of id and path filters that do not read one. */
    private static final byte[] EMPTY_BUFFER = new byte[0];

    /**
     * Identifier to buffer. Thread local because filtering is multithreaded and two threads can
     * load different components under the same identifier.
     */
    private static final ThreadLocal<Map<String, byte[]>> identifierToBufferThread = new ThreadLocal<>();

    /** Fallback for when the calling thread has no buffer for the identifier. */
    private static final Map<String, byte[]> identifierToBufferGlobal
            = Collections.synchronizedMap(createIdentifierToBufferMap());

    private static final StringTrieSearch pathSearchTree = new StringTrieSearch();
    private static final StringTrieSearch identifierSearchTree = new StringTrieSearch();
    private static volatile boolean indexed;

    private LithoFilter() {}

    /** Injection point, called from the application's onCreate by each patch that ships a filter. */
    public static void register(Filter filter) {
        filters.add(filter);
    }

    /** Builds the search trees on first use, once every patch has registered its filter. */
    private static synchronized void index() {
        if (indexed) return;
        filters.add(new CustomFilter());
        for (Filter filter : filters) {
            registerCallbacks(identifierSearchTree, filter, filter.identifierCallbacks, Filter.FilterContentType.IDENTIFIER);
            registerCallbacks(pathSearchTree, filter, filter.pathCallbacks, Filter.FilterContentType.PATH);
        }
        indexed = true;

        Logger.debug(() -> "Using: "
                + identifierSearchTree.numberOfPatterns() + " identifier filters"
                + " (" + identifierSearchTree.estimatedMemorySizeKb() + " KB), "
                + pathSearchTree.numberOfPatterns() + " path filters"
                + " (" + pathSearchTree.estimatedMemorySizeKb() + " KB)");
    }

    private static void registerCallbacks(StringTrieSearch searchTree, Filter filter,
                                          List<StringFilterGroup> groups, Filter.FilterContentType type) {
        String filterName = filter.getClass().getSimpleName();

        for (StringFilterGroup group : groups) {
            for (String pattern : group.filters) {
                searchTree.addPattern(pattern, (textSearched, matchedStartIndex, matchedLength, callbackParameter) -> {
                    if (!group.isEnabled()) return false;

                    Parameters parameters = (Parameters) callbackParameter;
                    final boolean isFiltered = filter.isFiltered(parameters.identifier, parameters.accessibility,
                            parameters.path, parameters.buffer, group, type, matchedStartIndex);

                    if (isFiltered) {
                        Logger.debug(() -> type == Filter.FilterContentType.IDENTIFIER
                                ? filterName + " filtered identifier: " + parameters.identifier
                                : filterName + " filtered path: " + parameters.path);
                    }

                    return isFiltered;
                });
            }
        }
    }

    private static Map<String, byte[]> createIdentifierToBufferMap() {
        // How many components are in flight at once is not knowable; this is a guess.
        final int maxSize = 100;
        return new LinkedHashMap<>(2 * maxSize) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                return size() > maxSize;
            }
        };
    }

    /** Matches only ASCII digits, unlike {@link Character#isDigit(char)}. */
    private static boolean isAsciiNumber(byte character) {
        return '0' <= character && character <= '9';
    }

    private static boolean isAsciiLowerCaseLetter(byte character) {
        return 'a' <= character && character <= 'z';
    }

    /** Injection point. Called off the main thread. */
    public static void setProtoBuffer(byte[] buffer) {
        // The identifier starts near the buffer start: the highest start index observed is 50 and
        // most are 30 to 40, while the buffer itself can reach 200kb. Searching the whole buffer
        // for every component would cost far more than it can find.
        if (buffer == null) return;
        final int maxBufferStartIndex = 500;

        int emlIndex = -1;
        final int emlStringLength = COMPONENT_EXTENSION_BYTES.length;
        final int lastIndexToCheck = Math.min(maxBufferStartIndex, buffer.length - emlStringLength);
        for (int i = 0; i <= lastIndexToCheck; i++) {
            boolean match = true;
            for (int j = 0; j < emlStringLength; j++) {
                if (buffer[i + j] != COMPONENT_EXTENSION_BYTES[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                emlIndex = i;
                break;
            }
        }

        if (emlIndex < 0) {
            return; // Not a buffer for a new litho component.
        }

        if (emlIndex == 0) return;
        int startIndex = emlIndex - 1;
        while (startIndex > 0) {
            final byte character = buffer[startIndex];
            if (isAsciiLowerCaseLetter(character) || isAsciiNumber(character) || character == '_') {
                startIndex--;
            } else {
                startIndex++;
                break;
            }
        }

        // Random data before the identifier can leave digits on the front of it.
        while (isAsciiNumber(buffer[startIndex])) {
            startIndex++;
        }

        int endIndex = -1;
        for (int i = emlIndex, length = buffer.length; i < length; i++) {
            if (buffer[i] == '|') {
                endIndex = i;
                break;
            }
        }
        if (endIndex < 0) {
            Logger.debug(() -> "Could not find the identifier in the buffer");
            return;
        }

        String identifier = new String(buffer, startIndex, endIndex - startIndex, StandardCharsets.US_ASCII);
        identifierToBufferGlobal.put(identifier, buffer);

        Map<String, byte[]> map = identifierToBufferThread.get();
        if (map == null) {
            map = createIdentifierToBufferMap();
            identifierToBufferThread.set(map);
        }
        map.put(identifier, buffer);
    }

    /**
     * Injection point. Called off the main thread, once per litho component.
     * <p>
     */
    public static boolean isFiltered(String identifier, String accessibilityId,
                                     String accessibilityText, StringBuilder pathBuilder) {
        try {
            if (identifier == null || identifier.isEmpty() || pathBuilder == null || pathBuilder.length() == 0) {
                return false;
            }
            if (!indexed) index();

            byte[] buffer = null;
            final int pipeIndex = identifier.indexOf('|');
            if (pipeIndex >= 0) {
                // No pipe means this is not an ".eml" identifier and no buffer is uniquely tied to
                // it. That happens for subcomponents, which buffer filtering does not reach.
                String identifierKey = identifier.substring(0, pipeIndex);

                Map<String, byte[]> map = identifierToBufferThread.get();
                if (map != null) {
                    buffer = map.get(identifierKey);
                }
                if (buffer == null) {
                    buffer = identifierToBufferGlobal.get(identifierKey);
                }
            }

            if (buffer == null) {
                // Some components never get a buffer, such as shorts_lockup_cell.eml on channel
                // profiles. Filter them on id and path alone rather than skipping them.
                buffer = EMPTY_BUFFER;
            }

            String accessibility = "";
            if (accessibilityId != null && !accessibilityId.isBlank()) {
                accessibility = accessibilityId;
            }
            if (accessibilityText != null && !accessibilityText.isBlank()) {
                accessibility = accessibility.isEmpty() ? accessibilityText : accessibility + '|' + accessibilityText;
            }

            Parameters parameters = new Parameters(identifier, pathBuilder.toString(), accessibility, buffer);
            Logger.debug(() -> "Searching " + parameters);

            return identifierSearchTree.matches(identifier, parameters)
                    || pathSearchTree.matches(parameters.path, parameters);
        } catch (Exception ex) {
            Logger.error(() -> "isFiltered failure: " + ex);
        }

        return false;
    }

    /** Injection point, for both the core pool size and the maximum thread count. */
    public static int layoutThreadCount(int original) {
        if (original != LAYOUT_THREAD_POOL_SIZE) {
            Logger.debug(() -> "Overriding litho layout threads from: " + original
                    + " to: " + LAYOUT_THREAD_POOL_SIZE);
        }
        return LAYOUT_THREAD_POOL_SIZE;
    }

    /** What the filters are handed, passed through the prefix search as the callback parameter. */
    private static final class Parameters {
        final String identifier;
        final String path;
        final String accessibility;
        final byte[] buffer;

        Parameters(String identifier, String path, String accessibility, byte[] buffer) {
            this.identifier = identifier;
            this.path = path;
            this.accessibility = accessibility;
            this.buffer = buffer;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(100);
            builder.append("ID: ");
            builder.append(identifier);
            if (!accessibility.isEmpty()) {
                builder.append(" Accessibility: ");
                builder.append(accessibility);
            }
            builder.append(" Path: ");
            builder.append(path);
            return builder.toString();
        }
    }
}
