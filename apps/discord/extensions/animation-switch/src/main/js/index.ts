// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Emoji animate with GIFs, so turning off GIF autoplay stops both.
export function withoutEmojiAnimation(args: any) {
    return args.options.gifAutoPlay === false ? { ...args, options: { ...args.options, animateEmoji: false } } : args;
}
