// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

exports.useTitle = function () {
    return "Animate chat GIFs and emoji";
};

exports.createMessageContent = function (original, args) {
    if (args.options.gifAutoPlay !== false) return original(args);
    return original({ ...args, options: { ...args.options, animateEmoji: false } });
};
