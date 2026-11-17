// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// A long stream sees many moderations, so each map keeps only the newest ones.
const MAX_ACTIONS = 500;

type Moderation = { kind?: string; targetMsgId?: string; userLogin?: string; durationSeconds?: number };
type ModAction = { modActionType: string; modDurationSeconds?: number };
type ChatMessage = { id?: string; user?: string; deleted?: boolean; modActionType?: string };
type RowProps = { message?: ChatMessage | null };
type ModeratedRange = { start: number; end: number };
type BodyProps = { strikethrough?: boolean; moderatedRanges?: readonly ModeratedRange[] | null };

// For viewers, Twitch's chat store marks a line deleted without the action that removed it, so these keep the actions.
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

// The chat line memoizes the message it passes, so this keeps one labeled copy per message object.
const labeledCopies = new WeakMap<object, ChatMessage>();

// A deleted row shows its text, with the action next to it, only when the message names that action.
export function withModeration(props: RowProps | null | undefined) {
    const message = props?.message;
    if (props == null || message == null || !message.deleted || message.modActionType != null) return props;
    let copy = labeledCopies.get(message);
    if (copy === undefined) {
        const action =
            (message.id && actionsByMessage.get(message.id)) ||
            (message.user && actionsByUser.get(message.user.toLowerCase())) ||
            { modActionType: "delete" };
        copy = { ...message, ...action };
        labeledCopies.set(message, copy);
    }
    return { ...props, message: copy };
}

// Twitch only compares each position with a range's ends, so one range that ends at the largest safe integer covers a message of any length.
// MessageBody caches its segments by the identity of this list, so every body shares the same one.
const wholeBody: readonly ModeratedRange[] = [{ start: 0, end: Number.MAX_SAFE_INTEGER }];

// Twitch strikes a deleted message's text through, so this draws it on AutoMod's red background instead.
export function withModeratedBody(props: BodyProps | null | undefined) {
    if (props?.strikethrough !== true) return props;
    return { ...props, strikethrough: false, moderatedRanges: wholeBody };
}
