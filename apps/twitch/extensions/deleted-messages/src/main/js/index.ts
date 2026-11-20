// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

const MAX_ACTIONS = 500;

// Twitch shows a timeout's length only next to the moderator's name, which IRC lines never carry.
// JavaScript hooks cannot read Reseam's string tables, so the stand-in stays English.
const MODERATOR_LABEL = "a moderator";

type Moderation = { kind?: string; targetMsgId?: string; userLogin?: string; durationSeconds?: number };
type ModAction = { modActionType: string; modDurationSeconds?: number };
type ChatMessage = { id?: string; user?: string; deleted?: boolean; modActionType?: string; modDurationSeconds?: number; modLogin?: string };
type RowProps = { message?: ChatMessage | null };
type ModeratedRange = { start: number; end: number };
type BodyProps = { strikethrough?: boolean; moderatedRanges?: readonly ModeratedRange[] | null };

// For viewers, the chat store marks a line deleted without the action that removed it.
const actionsByMessage = new Map<string, ModAction>();
const actionsByUser = new Map<string, ModAction>();

function remember(actions: Map<string, ModAction>, key: string, action: ModAction) {
    actions.delete(key);
    actions.set(key, action);
    if (actions.size > MAX_ACTIONS) actions.delete(actions.keys().next().value!);
}

export function rememberModeration(moderation: Moderation | null | undefined) {
    if (moderation?.kind === "delete" && moderation.targetMsgId) {
        remember(actionsByMessage, moderation.targetMsgId, { modActionType: "delete" });
    } else if ((moderation?.kind === "ban" || moderation?.kind === "timeout") && moderation.userLogin) {
        const action = { modActionType: moderation.kind, modDurationSeconds: moderation.durationSeconds };
        remember(actionsByUser, moderation.userLogin.toLowerCase(), action);
    }
}

function recordedAction(message: ChatMessage): ModAction {
    return (
        (message.id && actionsByMessage.get(message.id)) ||
        (message.user && actionsByUser.get(message.user.toLowerCase())) ||
        { modActionType: "delete" }
    );
}

function labeled(message: ChatMessage, showTimeoutLength: boolean) {
    let labeledMessage = message.modActionType == null ? { ...message, ...recordedAction(message) } : message;
    if (showTimeoutLength && labeledMessage.modActionType === "timeout" && labeledMessage.modDurationSeconds && !labeledMessage.modLogin) {
        labeledMessage = { ...labeledMessage, modLogin: MODERATOR_LABEL };
    }
    return labeledMessage;
}

// The chat line memoizes its message, so each message object keeps one labeled copy per Show timeout length value.
const labeledCopies = [new WeakMap<object, ChatMessage>(), new WeakMap<object, ChatMessage>()] as const;

// A deleted row shows its text only when the message names the action.
export function withModeration(props: RowProps | null | undefined, showTimeoutLength: boolean) {
    const message = props?.message;
    if (props == null || message == null || !message.deleted) return props;
    const copies = labeledCopies[showTimeoutLength ? 1 : 0];
    let labeledMessage = copies.get(message);
    if (labeledMessage === undefined) {
        labeledMessage = labeled(message, showTimeoutLength);
        copies.set(message, labeledMessage);
    }
    return labeledMessage === message ? props : { ...props, message: labeledMessage };
}

// One range covers a message of any length, since Twitch only compares positions with a range's ends.
// MessageBody caches its segments by this list's identity.
const wholeBody: readonly ModeratedRange[] = [{ start: 0, end: Number.MAX_SAFE_INTEGER }];

// Draws a deleted message on AutoMod's red background instead of striking it through.
export function withModeratedBody(props: BodyProps | null | undefined) {
    if (props?.strikethrough !== true) return props;
    return { ...props, strikethrough: false, moderatedRanges: wholeBody };
}
