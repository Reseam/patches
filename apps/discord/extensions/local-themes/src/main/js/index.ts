// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// The Nitro theme picker shows locked themes as a preview; this one applies them.
export function withoutPreview(props: any) {
    return props.themeSelector === "nitro" ? { ...props, isPreview: false } : props;
}
