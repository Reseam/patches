// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

const EMOJI_OR_STICKER = /^https:\/\/(cdn|media)\.discordapp\.(com|net)\/(emojis|stickers)\//;

export function withoutLinkCards(args: any) {
    const kept = args.message.embeds.filter((embed: { url?: string }) => EMOJI_OR_STICKER.test(embed.url ?? ""));
    return kept.length === 0
        ? { ...args, options: { ...args.options, renderEmbeds: false } }
        : { ...args, message: args.message.set("embeds", kept) };
}
