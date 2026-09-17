// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Searches for a group of different patterns using a trie (prefix tree).
 * Can significantly speed up searching for multiple patterns.
 */
public abstract class TrieSearch<T> {

    public interface PatternMatched<T> {
        /**
         * @param textSearched      Text that was searched.
         * @param matchedStartIndex Start index of the search text, where the pattern was matched.
         * @param matchedLength     Length of the match.
         * @param callbackParameter Optional parameter passed into {@link TrieSearch#matches(Object, Object)}.
         * @return True, if the search should stop here. If false, searching continues for other matches.
         */
        boolean patternMatched(T textSearched, int matchedStartIndex, int matchedLength, Object callbackParameter);
    }

    /**
     * A compressed tree path for a single pattern that shares no sibling nodes.
     * <p>
     * For the patterns "foobar", "football" and "feet" the tree holds three compressed paths
     * of "bar", "tball" and "eet", and children arrays only for the levels containing
     * 'f', 'o' and 'o'. Long patterns would otherwise cost one node per character.
     */
    private static final class CompressedPath<T> {
        final T pattern;
        final int patternStartIndex;
        final int patternLength;
        final PatternMatched<T> callback;

        CompressedPath(T pattern, int patternStartIndex, int patternLength, PatternMatched<T> callback) {
            this.pattern = pattern;
            this.patternStartIndex = patternStartIndex;
            this.patternLength = patternLength;
            this.callback = callback;
        }

        boolean matches(Node<T> enclosingNode, T searchText, int searchTextLength, int searchTextIndex,
                        Object callbackParameter) {
            if (searchTextLength - searchTextIndex < patternLength - patternStartIndex) {
                return false;
            }

            for (int i = searchTextIndex, j = patternStartIndex; j < patternLength; i++, j++) {
                if (enclosingNode.charValue(searchText, i) != enclosingNode.charValue(pattern, j)) {
                    return false;
                }
            }

            return callback == null || callback.patternMatched(searchText,
                    searchTextIndex - patternStartIndex, patternLength, callbackParameter);
        }
    }

    static abstract class Node<T> {
        private static final char ROOT_NODE_CHARACTER_VALUE = 0;
        private static final int CHILDREN_ARRAY_INCREASE_SIZE_INCREMENT = 2;

        /** Character this node represents. Ignored for the root node. */
        private final char nodeValue;

        /** Present only when no children array exists; callbacks can still end on this node. */
        private CompressedPath<T> leaf;

        /**
         * Child nodes, grown as needed and perfectly hashed: a character the array contains always
         * maps to index (character % length). Characters it does not contain can collide with one
         * it does, so the node's own character is compared.
         */
        private Node<T>[] children;

        private List<PatternMatched<T>> endOfPatternCallbacks;

        Node() {
            this.nodeValue = ROOT_NODE_CHARACTER_VALUE;
        }

        Node(char nodeCharacterValue) {
            this.nodeValue = nodeCharacterValue;
        }

        private void addPattern(T pattern, int patternIndex, int patternLength, PatternMatched<T> callback) {
            if (patternIndex == patternLength) {
                if (endOfPatternCallbacks == null) {
                    endOfPatternCallbacks = new ArrayList<>(1);
                }
                endOfPatternCallbacks.add(callback);
                return;
            }

            if (leaf != null) {
                // Push the existing leaf down one level, then add the parameter pattern.
                if (children != null) throw new IllegalStateException();
                //noinspection unchecked
                children = new Node[1];
                CompressedPath<T> existing = leaf;
                leaf = null;
                addPattern(existing.pattern, existing.patternStartIndex, existing.patternLength, existing.callback);
            } else if (children == null) {
                leaf = new CompressedPath<>(pattern, patternIndex, patternLength, callback);
                return;
            }

            final char character = charValue(pattern, patternIndex);
            final int arrayIndex = hashIndexForTableSize(children.length, character);
            Node<T> child = children[arrayIndex];
            if (child == null) {
                child = createNode(character);
                children[arrayIndex] = child;
            } else if (child.nodeValue != character) {
                child = createNode(character);
                expandChildArray(child);
            }
            child.addPattern(pattern, patternIndex + 1, patternLength, callback);
        }

        /** Resizes the children table until every node hashes to exactly one array index. */
        private void expandChildArray(Node<T> child) {
            int replacementArraySize = Objects.requireNonNull(children).length;
            while (true) {
                replacementArraySize += CHILDREN_ARRAY_INCREASE_SIZE_INCREMENT;
                //noinspection unchecked
                Node<T>[] replacement = new Node[replacementArraySize];
                addNodeToArray(replacement, child);

                boolean collision = false;
                for (Node<T> existingChild : children) {
                    if (existingChild != null && !addNodeToArray(replacement, existingChild)) {
                        collision = true;
                        break;
                    }
                }
                if (collision) continue;

                children = replacement;
                return;
            }
        }

        private static <T> boolean addNodeToArray(Node<T>[] array, Node<T> childToAdd) {
            final int insertIndex = hashIndexForTableSize(array.length, childToAdd.nodeValue);
            if (array[insertIndex] != null) {
                return false;
            }
            array[insertIndex] = childToAdd;
            return true;
        }

        private static int hashIndexForTableSize(int arraySize, char nodeValue) {
            return nodeValue % arraySize;
        }

        /**
         * Static and iterative to avoid recursion, since the JVM does not optimize tail calls and
         * this runs for every character of every litho path.
         *
         * @return If any pattern matched and its callback halted the search.
         */
        private static <T> boolean matches(final Node<T> startNode, final T searchText,
                                           int searchTextIndex, final int searchTextEndIndex,
                                           final Object callbackParameter) {
            Node<T> node = startNode;
            int currentMatchLength = 0;

            while (true) {
                CompressedPath<T> leaf = node.leaf;
                if (leaf != null && leaf.matches(startNode, searchText, searchTextEndIndex, searchTextIndex, callbackParameter)) {
                    return true;
                }

                List<PatternMatched<T>> callbacks = node.endOfPatternCallbacks;
                if (callbacks != null) {
                    final int matchStartIndex = searchTextIndex - currentMatchLength;
                    for (PatternMatched<T> callback : callbacks) {
                        if (callback == null) {
                            return true; // No callback means every match is valid.
                        }
                        if (callback.patternMatched(searchText, matchStartIndex, currentMatchLength, callbackParameter)) {
                            return true;
                        }
                    }
                }

                Node<T>[] children = node.children;
                if (children == null || searchTextIndex == searchTextEndIndex) {
                    return false;
                }

                // Read through the start node so the VM sees one receiver class for the whole walk.
                final char character = startNode.charValue(searchText, searchTextIndex);
                final int arrayIndex = hashIndexForTableSize(children.length, character);
                Node<T> child = children[arrayIndex];
                if (child == null || child.nodeValue != character) {
                    return false;
                }

                node = child;
                searchTextIndex++;
                currentMatchLength++;
            }
        }

        /** Estimated number of memory pointers used from this node down. */
        private int estimatedPointersUsed() {
            int pointers = 4; // Fields in this class.
            if (leaf != null) {
                pointers += 4; // Fields in the leaf.
            }
            if (endOfPatternCallbacks != null) {
                pointers += endOfPatternCallbacks.size();
            }
            if (children != null) {
                pointers += children.length;
                for (Node<T> child : children) {
                    if (child != null) {
                        pointers += child.estimatedPointersUsed();
                    }
                }
            }
            return pointers;
        }

        abstract Node<T> createNode(char nodeValue);

        abstract char charValue(T text, int index);

        abstract int textLength(T text);
    }

    private final Node<T> root;
    private final List<T> patterns = new ArrayList<>();

    TrieSearch(Node<T> root) {
        this.root = Objects.requireNonNull(root);
    }

    /** Adds a pattern that always matches when found. A zero length pattern does nothing. */
    public void addPattern(T pattern) {
        addPattern(pattern, root.textLength(pattern), null);
    }

    /** @param callback Decides whether searching halts when the pattern is found. */
    public void addPattern(T pattern, PatternMatched<T> callback) {
        addPattern(pattern, root.textLength(pattern), Objects.requireNonNull(callback));
    }

    private void addPattern(T pattern, int patternLength, PatternMatched<T> callback) {
        if (patternLength == 0) return;

        patterns.add(pattern);
        root.addPattern(pattern, 0, patternLength, callback);
    }

    public boolean matches(T textToSearch) {
        return matches(textToSearch, null);
    }

    /**
     * Searches the text for any substring matching any pattern in this tree.
     *
     * @return If any pattern matched and its callback halted searching.
     */
    public boolean matches(T textToSearch, Object callbackParameter) {
        if (textToSearch == null || patterns.isEmpty()) {
            return false;
        }
        final int endIndex = root.textLength(textToSearch);
        for (int i = 0; i < endIndex; i++) {
            if (Node.matches(root, textToSearch, i, endIndex, callbackParameter)) return true;
        }
        return false;
    }

    /** Estimated memory size in kilobytes. */
    public int estimatedMemorySizeKb() {
        if (patterns.isEmpty()) {
            return 0;
        }
        // Assume pointer compression or a 32-bit device.
        final int bytesPerPointer = 4;
        return (int) Math.ceil((bytesPerPointer * root.estimatedPointersUsed()) / 1024.0);
    }

    public int numberOfPatterns() {
        return patterns.size();
    }

    public List<T> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }
}
