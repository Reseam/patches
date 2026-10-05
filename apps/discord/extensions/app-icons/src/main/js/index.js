// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

// The icon rows and the icon picker both check isPremium against the account.
function asFree(icon) {
    return { ...icon, isPremium: false };
}

exports.getOfficialAlternateIcons = function (original) {
    return original().map(asFree);
};

exports.getLimitedAlternateIcons = function (original) {
    return original().map(asFree);
};

exports.getIconById = function (original, id) {
    return asFree(original(id));
};
