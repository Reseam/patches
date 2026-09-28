// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.instagram.navigation;

import java.util.ArrayList;
import java.util.List;

public final class NavigationTabs {
    private NavigationTabs() {}

    public static List<Object> filter(List<?> tabs, boolean hideReels, boolean hideCreate) {
        List<Object> result = new ArrayList<>(tabs.size());
        for (Object tab : tabs) {
            String name = ((Enum<?>) tab).name();
            boolean hidden = hideReels && name.equals("CLIPS") || hideCreate && (name.equals("CREATION") || name.equals("SHARE"));
            if (!hidden) result.add(tab);
        }
        return result;
    }
}
