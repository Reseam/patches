// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

exports.ThemePicker = function (original, props) {
    return original(props.themeSelector === "nitro" ? { ...props, isPreview: false } : props);
};

exports.shouldSync = function (original, scope) {
    return scope === "appearance" ? false : original(scope);
};
