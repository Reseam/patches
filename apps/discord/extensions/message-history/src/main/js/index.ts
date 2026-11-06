// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import ChannelMessages from "discord:lib/ChannelMessages.tsx";
import { resolveHighlightThemedColors } from "discord:modules/messages/native/renderer/RowGeneratorUtils.tsx";

const MAX_MESSAGES = 1000;
const MAX_EDITS = 3;
const MAX_EDIT_LENGTH = 2000;
const DELETED_MARKER = "​";

interface Shown {
    text: string;
    canRecordEdit: boolean;
    edits: string[];
    editedAt: string | null;
    deleted: boolean;
}

interface Message {
    channel_id: string;
    id: string;
    content: string;
}

const shownMessages = new Map<string, Shown>();

const messageKey = (channelId: string, messageId: string) => `${channelId}:${messageId}`;

// Only text this phone showed is kept, once per shown version. Map order is the eviction order, so a
// message moves to the end each time it is shown.
function recordShown(message: Message): Shown {
    const key = messageKey(message.channel_id, message.id);
    const shown = shownMessages.get(key) ?? {
        text: message.content,
        canRecordEdit: true,
        edits: [],
        editedAt: null,
        deleted: false,
    };
    shownMessages.delete(key);
    shownMessages.set(key, shown);
    if (shownMessages.size > MAX_MESSAGES) shownMessages.delete(shownMessages.keys().next().value!);
    if (shown.text !== message.content) shown.canRecordEdit = true;
    shown.text = message.content;
    return shown;
}

const canKeep = (channelId: string, messageId: string) =>
    shownMessages.has(messageKey(channelId, messageId)) && ChannelMessages.getOrCreate(channelId).has(messageId);

function keepDeleted(channelId: string, messageIds: string[]) {
    for (const id of messageIds) shownMessages.get(messageKey(channelId, id))!.deleted = true;
    // The row diff compares messages by value, so a kept message must change to be drawn again.
    const messages = messageIds.reduce(
        (all, id) =>
            all.update(id, (message: any) =>
                message.content.endsWith(DELETED_MARKER)
                    ? message
                    : message.set("content", message.content + DELETED_MARKER),
            ),
        ChannelMessages.getOrCreate(channelId),
    );
    ChannelMessages.commit(messages);
}

// Old versions show as code blocks, so they cannot mention anyone or create link cards.
function codeBlock(text: string): string {
    const longestRun = Math.max(2, ...(text.match(/`+/g) ?? []).map(run => run.length));
    const fence = "`".repeat(longestRun + 1);
    return `${fence}\n${text}\n${fence}`;
}

function displayText(content: string, shown: Shown): string {
    const deleted = shown.deleted ? "\n\n[deleted]" : "";
    const history = shown.edits.map((text, index) => `\n\n[previous edit ${index + 1}]\n${codeBlock(text)}`);
    return content + deleted + history.join("");
}

export function withHistory(args: any) {
    if (args.isInlineReplyPreview) return args;
    const shown = recordShown(args.message);
    if (args.isEditing || (!shown.deleted && shown.edits.length === 0)) return args;
    return { ...args, message: args.message.set("content", displayText(args.message.content, shown)) };
}

export function deletedHighlight(args: { message: Message; theme: unknown; isEditing: boolean }, highlight: unknown) {
    if (args.isEditing || shownMessages.get(messageKey(args.message.channel_id, args.message.id))?.deleted !== true) {
        return highlight;
    }
    const colors = resolveHighlightThemedColors(args.theme);
    return { backgroundColor: colors.automodBlockedBackgroundColor, gutterColor: colors.automodBlockedGutterColor };
}

// Kept text belongs to the account that saw it.
export function onDispatch(action: { type: string }) {
    if (action.type === "LOGOUT") shownMessages.clear();
}

// True when the message stays, marked deleted; Flux then redraws its row.
export function keepDeletedMessage(action: { local?: boolean; channelId: string; id: string }) {
    if (action.local === true || !canKeep(action.channelId, action.id)) return false;
    keepDeleted(action.channelId, [action.id]);
    return true;
}

let keptInBulkDelete = false;

export function withoutKeptMessages(action: { local?: boolean; channelId: string; ids: string[] }) {
    const kept = action.local === true ? [] : action.ids.filter(id => canKeep(action.channelId, id));
    keptInBulkDelete = kept.length > 0;
    if (!keptInBulkDelete) return action;
    keepDeleted(action.channelId, kept);
    return { ...action, ids: action.ids.filter(id => !kept.includes(id)) };
}

// Kept messages changed the store even when Discord removed nothing.
export function bulkDeleteResult(changed: unknown) {
    return keptInBulkDelete || changed;
}

export function recordEdit(message: Message, update: any, updated: Message) {
    const shown = shownMessages.get(messageKey(message.channel_id, message.id));
    const isTextEdit =
        update.edited_timestamp != null && typeof update.content === "string" && updated.content !== message.content;
    if (shown?.canRecordEdit && isTextEdit && String(update.edited_timestamp) !== shown.editedAt) {
        shown.editedAt = String(update.edited_timestamp);
        shown.edits = [...shown.edits, shown.text.slice(0, MAX_EDIT_LENGTH)].slice(-MAX_EDITS);
        shown.canRecordEdit = false;
    }
}
