// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

let checkingRealNitro = false;
let nitroLockedIds = null;

function emojiLink(name, id, animated) {
    const url = `https://cdn.discordapp.com/emojis/${id}.webp?size=48&name=${encodeURIComponent(name)}`;
    return `[${name}](${animated ? `${url}&animated=true` : url})`;
}

exports.canUseEmojisEverywhere = function (original, user) {
    return checkingRealNitro ? original(user) : true;
};

exports.canUseAnimatedEmojis = function (original, user) {
    return checkingRealNitro ? original(user) : true;
};

exports.getEmojiUnavailableReason = function (original, options) {
    if (!nitroLockedIds) return original(options);

    checkingRealNitro = true;
    let reason, reasonWithNitro;
    try {
        reason = original(options);
        reasonWithNitro = original({ ...options, bypassPremiumEmojiEntitlement: true });
    } finally {
        checkingRealNitro = false;
    }
    // Only Nitro locks become links. Role, server and permission locks stay.
    if (reason == null || reasonWithNitro != null) return reason;

    nitroLockedIds.add(String(options.emoji.id));
    return null;
};

exports.parse = function (original, channel, content) {
    const outer = nitroLockedIds;
    const lockedIds = nitroLockedIds = new Set();
    let message;
    try {
        message = original(channel, content);
    } finally {
        nitroLockedIds = outer;
    }
    if (lockedIds.size === 0) return message;

    // Backtick runs match first, so emoji inside code stay as written.
    const codeOrEmoji = /(`+)[\s\S]*?\1|<(a?):(\w+):(\d+)>/g;
    return {
        ...message,
        content: message.content.replace(codeOrEmoji, (token, code, animated, name, id) =>
            code || !lockedIds.has(id) ? token : emojiLink(name, id, animated)),
        validNonShortcutEmojis: message.validNonShortcutEmojis.filter(emoji => !lockedIds.has(String(emoji.id))),
    };
};
