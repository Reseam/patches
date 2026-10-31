// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { cloneElement, type ReactElement } from "react";
import MessageActions from "discord:actions/MessageActionCreators.tsx";
import { MessageFlags, MessageStates } from "discord:Constants.tsx";
import createMessage from "discord:modules/messages/createMessage.tsx";
import DraftStore, { DraftType } from "discord:stores/DraftStore.tsx";

// Shows the draft as an ephemeral message from the current user, which only this phone has and
// Discord lets the user dismiss.
function showPreview(channelId: string) {
    const content: string = DraftStore.getDraft(channelId, DraftType.ChannelMessage);
    if (content.trim() === "") return;
    MessageActions.receiveMessage(
        channelId,
        createMessage({ channelId, content, flags: MessageFlags.EPHEMERAL, state: MessageStates.SENT }),
    );
}

// Discord's action button drops props it does not know; the long press reaches its pressable.
export function withLongPress(pressable: ReactElement<any> | null, props: { onLongPress?: () => void }) {
    return pressable && props.onLongPress ? cloneElement(pressable, { onLongPress: props.onLongPress }) : pressable;
}

// The send button is the action button its renderer gives `onSendMessage` as `onPress`.
function withPreview(sendButton: ReactElement<any> | null, onSendMessage: unknown, channelId: string) {
    const button = sendButton?.props.children;
    if (sendButton == null || button?.props.onPress !== onSendMessage) return sendButton;
    return cloneElement(sendButton, {}, cloneElement(button, { onLongPress: () => showPreview(channelId) }));
}

export function sendButtonWithPreview(
    sendButton: ReactElement<any> | null,
    props: { onSendMessage: unknown; channel: { id: string } },
) {
    return withPreview(sendButton, props.onSendMessage, props.channel.id);
}

export function sendButtonItemWithPreview(
    sendButton: ReactElement<any> | null,
    props: { onSendMessage: unknown; channelId: string },
) {
    return withPreview(sendButton, props.onSendMessage, props.channelId);
}
