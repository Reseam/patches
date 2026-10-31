// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { PaintbrushThinIcon } from "discord:design/components/Icon/native/redesign/generated/PaintbrushThinIcon.tsx";
import { THEME_EDITOR_ROUTE, ThemeEditorScreen, THEMES_ROUTE, ThemesScreen } from "./screens";

// Discord reads rows from this frozen map on every lookup, so a copy with the theme rows replaces it.
export function addThemeRows(settings: { SETTING_RENDERER_CONFIG: Record<string, unknown> }) {
    settings.SETTING_RENDERER_CONFIG = Object.freeze({
        ...settings.SETTING_RENDERER_CONFIG,
        [THEMES_ROUTE]: {
            type: "route",
            parent: "APPEARANCE",
            IconComponent: PaintbrushThinIcon,
            useTitle: () => "Custom themes",
            useSearchTerms: () => ["Theme", "Colors", "Reseam"],
            screen: { route: THEMES_ROUTE, getComponent: () => ThemesScreen },
        },
        [THEME_EDITOR_ROUTE]: {
            type: "route",
            parent: THEMES_ROUTE,
            unsearchable: true,
            useTitle: () => "Edit theme",
            screen: { route: THEME_EDITOR_ROUTE, getComponent: () => ThemeEditorScreen },
        },
    });
}

/** Appearance settings with Custom themes after the theme picker. */
export function withThemesRow<List extends { sections: { settings: string[] }[] }>(list: List): List {
    const [theme, ...rest] = list.sections;
    return { ...list, sections: [{ ...theme!, settings: [...theme!.settings, THEMES_ROUTE] }, ...rest] };
}
