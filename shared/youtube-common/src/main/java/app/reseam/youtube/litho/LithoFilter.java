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
 * A component's buffer is the serialized element its tree was built from. Each tree is built in one
 * call from its root element, so a component whose identifier is the root's gets the root encoded,
 * once per tree and only when a filter reads it. Components built outside such a call fall back to
 * the last parsed buffer carrying their identifier, which is wrong when several share it, such as
 * the rows of one menu.
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

    /** The element tree the calling thread is building. */
    private static final ThreadLocal<Tree> currentTree = new ThreadLocal<>();

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
                            parameters.path, parameters.buffer(), group, type, matchedStartIndex);

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
        if (buffer == null) return;
        String identifier = identifierOf(buffer);
        if (identifier == null) return;

        identifierToBufferGlobal.put(identifier, buffer);
        Map<String, byte[]> map = identifierToBufferThread.get();
        if (map == null) {
            map = createIdentifierToBufferMap();
            identifierToBufferThread.set(map);
        }
        map.put(identifier, buffer);
    }

    /** The component identifier a buffer carries near its start, or null for other buffers. */
    private static String identifierOf(byte[] buffer) {
        // The identifier starts near the buffer start: the highest start index observed is 50 and
        // most are 30 to 40, while the buffer itself can reach 200kb. Searching the whole buffer
        // for every component would cost far more than it can find.
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

        if (emlIndex <= 0) return null; // Not a buffer for a new litho component.

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
            return null;
        }

        return new String(buffer, startIndex, endIndex - startIndex, StandardCharsets.US_ASCII);
    }

    /**
     * Injection point, on entering the build of the tree under a root element, with the identifier
     * of the component it becomes. Returns the enclosing tree, which {@link #exitTree} restores.
     */
    public static Object enterTree(Object root, String identifier) {
        Tree outer = currentTree.get();
        final int pipeIndex = identifier == null ? -1 : identifier.indexOf('|');
        currentTree.set(new Tree(root, pipeIndex < 0 ? null : identifier.substring(0, pipeIndex), outer));
        return outer;
    }

    /** Injection point, on leaving the build that {@link #enterTree} entered. */
    public static void exitTree(Object outer) {
        currentTree.set((Tree) outer);
    }

    /** The element serialized, or null for an element kind it cannot encode. The patch implements it. */
    public static byte[] encodeElement(Object element) {
        return null;
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

            String accessibility = "";
            if (accessibilityId != null && !accessibilityId.isBlank()) {
                accessibility = accessibilityId;
            }
            if (accessibilityText != null && !accessibilityText.isBlank()) {
                accessibility = accessibility.isEmpty() ? accessibilityText : accessibility + '|' + accessibilityText;
            }

            Parameters parameters = new Parameters(identifier, pathBuilder.toString(), accessibility);
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

    /** The component's buffer, or an empty one for components no buffer is tied to. */
    private static byte[] bufferFor(String identifier) {
        // No pipe means this is not an ".eml" identifier and no buffer is uniquely tied to it. That
        // happens for subcomponents, which buffer filtering does not reach.
        final int pipeIndex = identifier.indexOf('|');
        if (pipeIndex < 0) return EMPTY_BUFFER;
        String identifierKey = identifier.substring(0, pipeIndex);

        byte[] buffer = null;
        // Templates nest: a component belongs to the innermost enclosing tree rooted at its identifier.
        for (Tree tree = currentTree.get(); tree != null && buffer == null; tree = tree.outer) {
            buffer = tree.bufferFor(identifierKey);
        }
        if (buffer == null) {
            Map<String, byte[]> map = identifierToBufferThread.get();
            if (map != null) buffer = map.get(identifierKey);
        }
        if (buffer == null) buffer = identifierToBufferGlobal.get(identifierKey);
        // Some components never get a buffer, such as shorts_lockup_cell.eml on channel profiles.
        // Filter them on id and path alone rather than skipping them.
        return buffer == null ? EMPTY_BUFFER : buffer;
    }



    /** A tree being built. Its root is encoded on first use: most trees are never asked. */
    private static final class Tree {
        private final Object root;
        private final String identifierKey;
        final Tree outer;
        private boolean encoded;
        private byte[] bytes;

        Tree(Object root, String identifierKey, Tree outer) {
            this.root = root;
            this.identifierKey = identifierKey;
            this.outer = outer;
        }

        byte[] bufferFor(String identifierKey) {
            if (!identifierKey.equals(this.identifierKey)) return null;
            if (!encoded) {
                encoded = true;
                bytes = encodeElement(root);
                // A nested template can be built under its parent's context; its root names itself.
                String declared = bytes == null ? null : identifierOf(bytes);
                if (declared != null && !declared.equals(identifierKey)) bytes = null;
            }
            return bytes;
        }
    }

    /** What the filters are handed, passed through the prefix search as the callback parameter. */
    private static final class Parameters {
        final String identifier;
        final String path;
        final String accessibility;
        private byte[] buffer;

        Parameters(String identifier, String path, String accessibility) {
            this.identifier = identifier;
            this.path = path;
            this.accessibility = accessibility;
        }

        /** Resolved on the first filter that matched, on the thread building the component. */
        byte[] buffer() {
            if (buffer == null) buffer = bufferFor(identifier);
            return buffer;
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
