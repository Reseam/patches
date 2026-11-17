// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type RowProps = { message?: { hidden?: boolean } | null };

// Twitch keeps a blocked user's message as a placeholder only in channels you moderate, and drops it everywhere else.
export function isHiddenMessage(props: RowProps | null | undefined) {
    return props?.message?.hidden === true;
}
