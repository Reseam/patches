// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

const pickerFeatures = new Set(["emojisEverywhere", "stickersEverywhere"]);

exports.PremiumFeatureUpsell = function (original, props) {
    return pickerFeatures.has(props.featureName) ? null : original(props);
};
