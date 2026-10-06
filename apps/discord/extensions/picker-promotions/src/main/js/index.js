// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

const pickerFeatures = new Set(["emojisEverywhere", "stickersEverywhere"]);

exports.PremiumFeatureUpsell = function (original, props) {
    return pickerFeatures.has(props.featureName) ? null : original(props);
};
