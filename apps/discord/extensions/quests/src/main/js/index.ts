// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// The profile action bar shows Quests to eligible users. Settings sits in the same bar and stays.
let renderingProfileActions = false;

export function enterProfileActions() {
    renderingProfileActions = true;
}

export function leaveProfileActions() {
    renderingProfileActions = false;
}

export function isRenderingProfileActions() {
    return renderingProfileActions;
}
