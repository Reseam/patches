// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type LineProps = { line?: { sourceRoomID?: string | null } | null; channelID?: string | null };

export function isFromOtherChannel(props: LineProps | null | undefined) {
    const sourceRoomID = props?.line?.sourceRoomID;
    const channelID = props?.channelID;
    return !!sourceRoomID && !!channelID && sourceRoomID !== channelID;
}
