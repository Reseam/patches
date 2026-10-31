// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { NativeModules } from "react-native";

const RESEAM_SETTINGS = "RESEAM_SETTINGS";

// Discord reads rows from this frozen map on every lookup, so a copy with the Reseam row replaces it.
export function addReseamRow(settings: { SETTING_RENDERER_CONFIG: Record<string, any> }) {
    const rows = settings.SETTING_RENDERER_CONFIG;
    settings.SETTING_RENDERER_CONFIG = Object.freeze({
        ...rows,
        [RESEAM_SETTINGS]: {
            type: "pressable",
            parent: null,
            useTitle: () => "Reseam Settings",
            IconComponent: rows.ADVANCED.IconComponent,
            onPress: () => NativeModules.ReseamSettings.openSettings("reseam-patches"),
            useSearchTerms: () => ["Reseam", "Patches"],
            withArrow: true,
        },
    });
}

export function withReseamSection<List extends { sections: { settings: string[] }[] }>(list: List): List {
    return { ...list, sections: [{ label: "Reseam", settings: [RESEAM_SETTINGS] }, ...list.sections] };
}
