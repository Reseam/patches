// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// The theatre applies its saved quality once the player reads as ready.
export function withoutSavedQuality(props: object | null | undefined) {
    return { ...props, isPlayerReady: false };
}
