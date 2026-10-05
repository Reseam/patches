// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

let renderingProfileActions = false;

exports.useMobileQuestDockHeight = function () {
    return 0;
};

// The profile action bar shows Quests to eligible users. Settings sits in the same bar and stays.
exports.profileActions = function (original, props) {
    const outer = renderingProfileActions;
    renderingProfileActions = true;
    try {
        return original(props);
    } finally {
        renderingProfileActions = outer;
    }
};

exports.getIsEligibleForQuests = function (original) {
    return renderingProfileActions ? false : original();
};
