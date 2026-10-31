// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { EmojiDisabledReasons } from "discord:modules/emojis/EmojiConstants.tsx";
import { isUnusableRoleSubscriptionEmoji } from "discord:modules/guild_role_subscriptions/RoleSubscriptionEmojiUtils.tsx";

let checkingRealNitro = false;
const parses: Set<string>[] = [];

export function isCheckingRealNitro() {
    return checkingRealNitro;
}

export function checkRealNitro() {
    checkingRealNitro = parses.length > 0;
}

// Only Nitro locks become links. Role subscription emoji stay locked, as they do with Nitro.
export function unlessNitroLocked(reason: unknown, options: any) {
    checkingRealNitro = false;
    const lockedIds = parses.at(-1);
    if (lockedIds === undefined || reason !== EmojiDisabledReasons.PREMIUM_LOCKED) return reason;
    const guildId = options.guildId === undefined ? options.channel?.getGuildId() : options.guildId;
    if (isUnusableRoleSubscriptionEmoji(options.emoji, guildId)) return reason;
    lockedIds.add(String(options.emoji.id));
    return null;
}

function emojiLink(name: string, id: string, animated: string): string {
    const url = `https://cdn.discordapp.com/emojis/${id}.webp?size=48&name=${encodeURIComponent(name)}`;
    return `[${name}](${animated ? `${url}&animated=true` : url})`;
}

// Backtick runs match first, so emoji inside code stay as written.
const CODE_OR_EMOJI = /(`+)[\s\S]*?\1|<(a?):(\w+):(\d+)>/g;

export function beginParse() {
    parses.push(new Set());
}

export function withEmojiLinks(message: any) {
    const lockedIds = parses.pop()!;
    if (lockedIds.size === 0) return message;
    return {
        ...message,
        content: message.content.replace(
            CODE_OR_EMOJI,
            (token: string, code: string | undefined, animated: string, name: string, id: string) =>
                code || !lockedIds.has(id) ? token : emojiLink(name, id, animated),
        ),
        validNonShortcutEmojis: message.validNonShortcutEmojis.filter(
            (emoji: { id: string }) => !lockedIds.has(String(emoji.id)),
        ),
    };
}
