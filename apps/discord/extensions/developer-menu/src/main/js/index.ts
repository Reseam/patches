// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Discord hides the Dev Tools row behind a staff check it passes as usePredicate.
export function withDevTools(config: any) {
    const isDevTools =
        config.withArrow === true && typeof config.onPress === "function" && config.onPress.name === "navigateToDevTools";
    return isDevTools ? { ...config, usePredicate: () => true } : config;
}
