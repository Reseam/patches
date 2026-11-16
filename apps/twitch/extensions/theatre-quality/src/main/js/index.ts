// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// The theatre applies the quality it saved once its player is ready, so a copy that never reads as ready skips that step.
export function withoutSavedQuality(props: object | null | undefined) {
    return { ...props, isPlayerReady: false };
}
