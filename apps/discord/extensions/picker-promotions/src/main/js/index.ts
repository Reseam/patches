// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

const PICKER_FEATURES = new Set(["emojisEverywhere", "stickersEverywhere"]);

export function isPickerUpsell(props: { featureName: string }) {
    return PICKER_FEATURES.has(props.featureName);
}
