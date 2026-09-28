// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.reddit.ads;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class FeedAds {
    private static volatile Set<String> adClasses;

    private FeedAds() {}

    public static boolean isAd(Object element) {
        if (element == null) return false;
        Set<String> classes = adClasses;
        if (classes == null) adClasses = classes = new HashSet<>(Arrays.asList(adClassNames().split(",")));
        return classes.contains(element.getClass().getName());
    }

    // R8 renames the ad element classes, so the patch writes their names in.
    static String adClassNames() {
        throw new UnsupportedOperationException("implemented by the hide feed ads patch");
    }
}
