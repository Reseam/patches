// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

export function withSeconds(content: any, args: any) {
    if (typeof content.timestamp !== "string") return content;
    const seconds = String(new Date(args.message.timestamp.valueOf()).getSeconds()).padStart(2, "0");
    return { ...content, timestamp: content.timestamp.replace(/\b(\d{1,2}:\d{2})(?![:\d])/, `$1:${seconds}`) };
}
