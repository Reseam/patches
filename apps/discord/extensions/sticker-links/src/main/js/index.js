// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Discord's StickerSendability values.
const SENDABLE = 0;
const SENDABLE_WITH_PREMIUM = 1;
// PNG, APNG and GIF. Lottie stickers have no image file to link to.
const imageFormats = new Set([1, 2, 4]);
const MAX_LINKABLE_STICKERS = 256;

const linkableStickers = new Map();
let stickerAssetUrl = null;

function stickerKey(channelId, stickerId) {
    return `${channelId}:${stickerId}`;
}

// The URL Discord loads for the full sticker, so GIF stickers keep their format.
function imageUrl(sticker) {
    if (stickerAssetUrl === null) return null;
    const url = stickerAssetUrl(sticker, { isPreview: false, size: 256 });
    return typeof url === "string" && url.startsWith("https://") ? url : null;
}

function markdownLink(sticker, url) {
    const name = String(sticker.name || "sticker").replace(/[\\[\]\r\n]/g, "_");
    return `[${name}](${url.replace(/\(/g, "%28").replace(/\)/g, "%29")})`;
}

exports.getStickerAssetUrl = function (original, sticker, options) {
    stickerAssetUrl = original;
    return original(sticker, options);
};

exports.getStickerSendability = function (original, sticker, user, channel) {
    const sendability = original(sticker, user, channel);
    if (channel == null) return sendability;
    const key = stickerKey(channel.id, sticker.id);
    linkableStickers.delete(key);
    // Only Nitro locks become links. Permission and availability locks stay.
    if (sendability !== SENDABLE_WITH_PREMIUM || !imageFormats.has(sticker.format_type) || imageUrl(sticker) === null) {
        return sendability;
    }
    if (linkableStickers.size >= MAX_LINKABLE_STICKERS) linkableStickers.delete(linkableStickers.keys().next().value);
    linkableStickers.set(key, sticker);
    return SENDABLE;
};

exports.sendStickers = function (original, channelId, stickerIds, content = "", options, tts = false) {
    const links = [];
    const remaining = [];
    for (const id of stickerIds) {
        const sticker = linkableStickers.get(stickerKey(channelId, id));
        const url = sticker && imageUrl(sticker);
        if (url) links.push(markdownLink(sticker, url));
        else remaining.push(id);
    }
    if (links.length === 0) return original(channelId, stickerIds, content, options, tts);
    const withLinks = text => (text ? `${text}\n` : "") + links.join("\n");
    const message = typeof content === "string" ? withLinks(content) : { ...content, content: withLinks(content.content) };
    // Without sticker IDs Discord sends a plain message.
    return original(channelId, remaining.length ? remaining : undefined, message, options, tts);
};
