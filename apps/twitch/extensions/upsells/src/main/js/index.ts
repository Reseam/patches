// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Subscribe buttons also show your own subscription, which stays.
export function isSubscribed(props: { isSubscribed?: boolean } | null | undefined) {
    return props?.isSubscribed === true;
}
