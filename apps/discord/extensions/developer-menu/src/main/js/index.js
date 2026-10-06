// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Discord hides the Dev Tools row behind a staff check it passes as usePredicate.
exports.createPressable = function (original, config) {
    const isDevTools = config.withArrow === true && typeof config.onPress === "function" &&
        config.onPress.name === "navigateToDevTools";
    return original(isDevTools ? { ...config, usePredicate: () => true } : config);
};
