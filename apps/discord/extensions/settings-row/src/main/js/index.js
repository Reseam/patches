// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

const RESEAM_SETTINGS = "RESEAM_SETTINGS";

function openReseamSettings() {
    globalThis.nativeModuleProxy.ReseamSettings.openSettings();
}

// Discord reads rows from this frozen map on every lookup, so a copy with the Reseam row replaces it.
exports.settingRows = function (original, global, require, importDefault, importAll, module, moduleExports, dependencyMap) {
    const result = original(global, require, importDefault, importAll, module, moduleExports, dependencyMap);
    const rows = moduleExports.SETTING_RENDERER_CONFIG;
    moduleExports.SETTING_RENDERER_CONFIG = Object.freeze({
        ...rows,
        [RESEAM_SETTINGS]: {
            type: "pressable",
            parent: null,
            useTitle: () => "Reseam Settings",
            IconComponent: rows.ADVANCED.IconComponent,
            onPress: openReseamSettings,
            useSearchTerms: () => ["Reseam", "Patches"],
            withArrow: true,
        },
    });
    return result;
};

exports.settingsList = function (original) {
    const list = original();
    const sections = [...list.sections];
    const appSettings = sections.findIndex(section => section.settings.includes("ADVANCED"));
    if (appSettings === -1) {
        throw new Error("Discord settings have no App Settings section");
    }
    sections.splice(appSettings + 1, 0, { label: "Reseam", settings: [RESEAM_SETTINGS] });
    return { ...list, sections };
};
