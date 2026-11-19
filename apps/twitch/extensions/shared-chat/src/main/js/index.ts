// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type LineProps = { line?: { sourceRoomID?: string | null } | null; channelID?: string | null };

// In a shared chat, every line names the channel it came from, and the chat names the channel you are watching.
export function isFromOtherChannel(props: LineProps | null | undefined) {
    const sourceRoomID = props?.line?.sourceRoomID;
    const channelID = props?.channelID;
    return !!sourceRoomID && !!channelID && sourceRoomID !== channelID;
}
