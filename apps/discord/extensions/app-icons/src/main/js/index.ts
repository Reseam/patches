// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// The icon rows and the icon picker both check isPremium against the account.
export function asFree<Icon extends object>(icon: Icon) {
    return { ...icon, isPremium: false };
}

export function allFree<Icon extends object>(icons: Icon[]) {
    return icons.map(asFree);
}
