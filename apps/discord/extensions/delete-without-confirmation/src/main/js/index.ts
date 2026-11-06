// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import MessageActions from "discord:actions/MessageActionCreators.tsx";
import { MessageStates } from "discord:Constants.tsx";
import { intl, t } from "discord:intl/index.native.tsx";
import ActionSheets from "discord:modules/action_sheet/native/ActionSheetActionCreators.tsx";

interface MessageOption {
    label: string;
    message: { id: string; state: string };
    channel: { id: string };
    onActionExecuted?: (label: string) => void;
    disabled?: boolean;
}

// True when the picked option was Delete Message, which the menu knows only by its translated label,
// `t.xwMqD7`. Messages still sending or failed are already deleted without asking.
export function deleteMessage({ label, message, channel, onActionExecuted, disabled }: MessageOption) {
    if (disabled || label !== intl.string(t.xwMqD7) || message.state !== MessageStates.SENT) return false;
    ActionSheets.hideActionSheet();
    MessageActions.deleteMessage(channel.id, message.id, false);
    onActionExecuted?.(label);
    return true;
}
