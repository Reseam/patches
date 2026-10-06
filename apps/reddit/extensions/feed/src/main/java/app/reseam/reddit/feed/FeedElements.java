// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.feed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FeedElements {
    private static final Map<String, Set<String>> classSets = new ConcurrentHashMap<>();

    private FeedElements() {}

    // R8 renames the element classes, so the patch passes their names in, comma separated.
    public static boolean isAny(Object element, String classNames) {
        if (element == null) return false;
        Set<String> classes = classSets.computeIfAbsent(classNames, names -> new HashSet<>(Arrays.asList(names.split(","))));
        return classes.contains(element.getClass().getName());
    }

    public static List<Object> without(List<?> elements, String classNames) {
        List<Object> kept = new ArrayList<>(elements.size());
        for (Object element : elements) {
            if (!isAny(element, classNames)) kept.add(element);
        }
        return kept;
    }
}
