// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

exports.useTitle = function () {
    return "Animate chat GIFs and emoji";
};

exports.createMessageContent = function (original, args) {
    if (args.options.gifAutoPlay !== false) return original(args);
    return original({ ...args, options: { ...args.options, animateEmoji: false } });
};
