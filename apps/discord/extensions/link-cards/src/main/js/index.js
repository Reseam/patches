// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

function isEmojiOrSticker(embed) {
    return /^https:\/\/(cdn|media)\.discordapp\.(com|net)\/(emojis|stickers)\//.test(embed.url || "");
}

exports.createMessageContent = function (original, args) {
    const kept = args.message.embeds.filter(isEmojiOrSticker);
    if (kept.length === 0) {
        return original({ ...args, options: { ...args.options, renderEmbeds: false } });
    }
    return original({ ...args, message: args.message.set("embeds", kept) });
};
