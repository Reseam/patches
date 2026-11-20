// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type Moderation = { kind?: string };

export function isChatClear(moderation: Moderation | null | undefined) {
    return moderation?.kind === "clear";
}
