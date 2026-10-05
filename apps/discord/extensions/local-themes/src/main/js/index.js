// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

exports.ThemePicker = function (original, props) {
    return original(props.themeSelector === "nitro" ? { ...props, isPreview: false } : props);
};

exports.shouldSync = function (original, scope) {
    return scope === "appearance" ? false : original(scope);
};
