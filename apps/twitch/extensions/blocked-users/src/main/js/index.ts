// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type RowProps = { message?: { hidden?: boolean } | null };

// Twitch keeps a placeholder for a blocked user's message only in channels you moderate.
export function isHiddenMessage(props: RowProps | null | undefined) {
    return props?.message?.hidden === true;
}
