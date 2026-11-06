// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import MessageActions from "discord:actions/MessageActionCreators.tsx";
import { canReplyToMessage } from "discord:modules/replies/canReplyToMessage.tsx";
import UserStore from "discord:stores/UserStore.tsx";

// The chat list reports a tap that follows a double tap only when its 300 ms double tap window
// closes, so a double tap waits this long for a third tap before it starts the reply. Replying at
// once would open the keyboard and move the message from under the third tap.
const THIRD_TAP_WAIT_MS = 650;

interface TapEvent {
    nativeEvent: { messageId: string };
}

interface Message {
    id: string;
    channel_id: string;
    author: { id: string };
}

interface MessageData {
    message: Message;
    messageChannel: unknown;
}

interface MessagesHandlers {
    getMessageData(messageId: string): MessageData | null;
    handleTapMessage(event: TapEvent): void;
    handleDoubleTapMessage(event: TapEvent): void;
    handleInitiateReply(event: { nativeEvent: { messageId: string; triggerHaptic: boolean } }): void;
}

let pendingReply: { message: Message; timer: ReturnType<typeof setTimeout> } | null = null;

const isOwnReplyable = ({ message, messageChannel }: MessageData) =>
    message.author.id === UserStore.getCurrentUser().id && canReplyToMessage(messageChannel, message);

function cancelPendingReply() {
    if (pendingReply == null) return null;
    clearTimeout(pendingReply.timer);
    const { message } = pendingReply;
    pendingReply = null;
    return message;
}

export function withQuickActions(handlers: MessagesHandlers) {
    const { handleTapMessage, handleDoubleTapMessage } = handlers;

    handlers.handleDoubleTapMessage = event => {
        const data = handlers.getMessageData(event.nativeEvent.messageId);
        if (data == null || !isOwnReplyable(data)) return handleDoubleTapMessage(event);
        cancelPendingReply();
        const reply = () => {
            pendingReply = null;
            handlers.handleInitiateReply({ nativeEvent: { messageId: data.message.id, triggerHaptic: true } });
        };
        pendingReply = { message: data.message, timer: setTimeout(reply, THIRD_TAP_WAIT_MS) };
    };

    handlers.handleTapMessage = event => {
        const message = pendingReply?.message.id === event.nativeEvent.messageId ? cancelPendingReply() : null;
        if (message != null) MessageActions.deleteMessage(message.channel_id, message.id, false);
        handleTapMessage(event);
    };

    return handlers;
}
