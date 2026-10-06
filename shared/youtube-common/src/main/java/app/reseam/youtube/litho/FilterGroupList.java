// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;

/** Several filter groups searched together through one prefix tree. */
public abstract class FilterGroupList<V, T extends FilterGroup<V>> implements Iterable<T> {

    private final List<T> filterGroups = new ArrayList<>();
    private final TrieSearch<V> search = createSearchGraph();

    @SafeVarargs
    public final void addAll(final T... groups) {
        filterGroups.addAll(Arrays.asList(groups));

        for (T group : groups) {
            for (V pattern : group.filters) {
                search.addPattern(pattern, (textSearched, matchedStartIndex, matchedLength, callbackParameter) -> {
                    if (!group.isEnabled()) return false;

                    FilterGroup.FilterGroupResult result = (FilterGroup.FilterGroupResult) callbackParameter;
                    result.setValues(matchedStartIndex, matchedLength);
                    return true;
                });
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        return filterGroups.iterator();
    }

    public FilterGroup.FilterGroupResult check(V stack) {
        FilterGroup.FilterGroupResult result = new FilterGroup.FilterGroupResult();
        search.matches(stack, result);
        return result;
    }

    protected abstract TrieSearch<V> createSearchGraph();

    public static final class StringFilterGroupList extends FilterGroupList<String, StringFilterGroup> {
        protected StringTrieSearch createSearchGraph() {
            return new StringTrieSearch();
        }
    }

    /**
     * For a single byte pattern {@link ByteArrayFilterGroup#check(byte[])} is slightly better:
     * KMP beats a prefix tree when there is only one pattern to find.
     */
    public static final class ByteArrayFilterGroupList extends FilterGroupList<byte[], ByteArrayFilterGroup> {
        protected ByteTrieSearch createSearchGraph() {
            return new ByteTrieSearch();
        }
    }
}
