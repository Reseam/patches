// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type TrayProps = { tray?: { kind?: string } | null };

export function isUniqueChat(props: TrayProps | null | undefined) {
    return props?.tray?.kind === "uniqueChat";
}
