// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { StickerSendability } from "discord:modules/stickers/StickerSendability.tsx";
import { StickerFormat } from "discord:modules/stickers/StickersTypes.tsx";
import { getStickerAssetUrl } from "discord:modules/stickers/StickersUtils.tsx";

const MAX_LINKABLE_STICKERS = 256;

const linkableStickers = new Map<string, any>();

const stickerKey = (channelId: string, stickerId: string) => `${channelId}:${stickerId}`;

// Lottie stickers have no image file to link to.
const hasImage = (sticker: any) =>
    [StickerFormat.PNG, StickerFormat.APNG, StickerFormat.GIF].includes(sticker.format_type);

// The URL Discord loads for the full sticker, so GIF stickers keep their format.
function imageUrl(sticker: any): string | null {
    const url = getStickerAssetUrl(sticker, { isPreview: false, size: 256 });
    return typeof url === "string" && url.startsWith("https://") ? url : null;
}

function markdownLink(sticker: any, url: string): string {
    const name = String(sticker.name || "sticker").replace(/[\\[\]\r\n]/g, "_");
    return `[${name}](${url.replace(/\(/g, "%28").replace(/\)/g, "%29")})`;
}

// Only Nitro locks become links. Permission and availability locks stay.
export function sendability(sendability: number, sticker: any, channel: any) {
    if (channel == null) return sendability;
    const key = stickerKey(channel.id, sticker.id);
    linkableStickers.delete(key);
    if (sendability !== StickerSendability.SENDABLE_WITH_PREMIUM || !hasImage(sticker) || imageUrl(sticker) === null) {
        return sendability;
    }
    if (linkableStickers.size >= MAX_LINKABLE_STICKERS) linkableStickers.delete(linkableStickers.keys().next().value!);
    linkableStickers.set(key, sticker);
    return StickerSendability.SENDABLE;
}

function links(channelId: string, stickerIds: string[]): string[] {
    return stickerIds.flatMap(id => {
        const sticker = linkableStickers.get(stickerKey(channelId, id));
        const url = sticker && imageUrl(sticker);
        return url ? [markdownLink(sticker, url)] : [];
    });
}

export function withStickerLinks(channelId: string, stickerIds: string[], content: any = "") {
    const added = links(channelId, stickerIds);
    if (added.length === 0) return content;
    const withLinks = (text: string) => (text ? `${text}\n` : "") + added.join("\n");
    return typeof content === "string" ? withLinks(content) : { ...content, content: withLinks(content.content) };
}

// Without stickers left, Discord sends a plain message.
export function unlinkedStickers(channelId: string, stickerIds: string[]) {
    const remaining = stickerIds.filter(id => links(channelId, [id]).length === 0);
    return remaining.length === 0 ? undefined : remaining;
}
