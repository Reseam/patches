// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

exports.createMessageContent = function (original, args) {
    const content = original(args);
    if (typeof content.timestamp !== "string") return content;
    const seconds = String(new Date(args.message.timestamp.valueOf()).getSeconds()).padStart(2, "0");
    // Adds seconds to H:MM and HH:MM times; locales without that pattern stay unchanged.
    return { ...content, timestamp: content.timestamp.replace(/\b(\d{1,2}:\d{2})(?![:\d])/, `$1:${seconds}`) };
};
