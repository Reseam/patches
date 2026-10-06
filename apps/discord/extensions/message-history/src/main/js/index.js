// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

const MAX_MESSAGES = 1000;
const MAX_EDITS = 3;
const MAX_EDIT_LENGTH = 2000;
const DELETED_MARKER = "\u200B";

const shownMessages = new Map();
// Discord's own channel message reads and writes, captured on first use. Reading
// through them keeps kept rows in step with every change Discord makes itself.
let readMessages = null;
let commitMessages = null;

function messageKey(channelId, messageId) {
    return `${channelId}:${messageId}`;
}

// Only text this phone showed is kept, once per shown version. Map order is the
// eviction order, so a message moves to the end each time it is shown.
function recordShown(message) {
    const key = messageKey(message.channel_id, message.id);
    const shown = shownMessages.get(key) ??
        { text: message.content, canRecordEdit: true, edits: [], editedAt: null, deleted: false };
    shownMessages.delete(key);
    shownMessages.set(key, shown);
    if (shownMessages.size > MAX_MESSAGES) shownMessages.delete(shownMessages.keys().next().value);
    if (shown.text !== message.content) shown.canRecordEdit = true;
    shown.text = message.content;
    return shown;
}

function canKeep(channelId, messageId) {
    return readMessages !== null && commitMessages !== null && shownMessages.has(messageKey(channelId, messageId)) &&
        readMessages(channelId).has(messageId);
}

function keepDeleted(channelId, messageIds) {
    for (const id of messageIds) shownMessages.get(messageKey(channelId, id)).deleted = true;
    // The row diff compares messages by value, so a kept message must change to be drawn again.
    const messages = messageIds.reduce((all, id) => all.update(id, message =>
        message.content.endsWith(DELETED_MARKER) ? message : message.set("content", message.content + DELETED_MARKER)),
    readMessages(channelId));
    commitMessages(messages);
}

// Old versions show as code blocks, so they cannot mention anyone or create link cards.
function codeBlock(text) {
    const longestRun = Math.max(2, ...(text.match(/`+/g) || []).map(run => run.length));
    const fence = "`".repeat(longestRun + 1);
    return `${fence}\n${text}\n${fence}`;
}

function displayText(content, shown) {
    const deleted = shown.deleted ? "\n\n[deleted]" : "";
    const history = shown.edits.map((text, index) => `\n\n[previous edit ${index + 1}]\n${codeBlock(text)}`).join("");
    return content + deleted + history;
}

exports.getOrCreate = function (original, channelId) {
    readMessages = original;
    return original(channelId);
};

exports.commit = function (original, messages) {
    commitMessages = original;
    return original(messages);
};

exports.createMessageContent = function (original, args) {
    if (args.isInlineReplyPreview) return original(args);
    const shown = recordShown(args.message);
    if (args.isEditing || (!shown.deleted && shown.edits.length === 0)) return original(args);
    return original({ ...args, message: args.message.set("content", displayText(args.message.content, shown)) });
};

// Kept text belongs to the account that saw it.
exports._dispatch = function (original, action, ...rest) {
    if (action.type === "LOGOUT") shownMessages.clear();
    return original(action, ...rest);
};

// Returning true tells Flux the store changed, so the kept row is drawn again with its marker.
exports.handleMessageDelete = function (original, action) {
    if (action.local === true || !canKeep(action.channelId, action.id)) return original(action);
    keepDeleted(action.channelId, [action.id]);
    return true;
};

exports.handleMessageDeleteBulk = function (original, action) {
    if (action.local === true) return original(action);
    const kept = action.ids.filter(id => canKeep(action.channelId, id));
    if (kept.length === 0) return original(action);
    keepDeleted(action.channelId, kept);
    const removed = action.ids.filter(id => !kept.includes(id));
    if (removed.length > 0) original({ ...action, ids: removed });
    return true;
};

exports.updateMessageRecord = function (original, message, update) {
    const shown = shownMessages.get(messageKey(message.channel_id, message.id));
    const updated = original(message, update);
    const isTextEdit = update.edited_timestamp != null && typeof update.content === "string" && updated.content !== message.content;
    if (shown !== undefined && shown.canRecordEdit && isTextEdit && String(update.edited_timestamp) !== shown.editedAt) {
        shown.editedAt = String(update.edited_timestamp);
        shown.edits = [...shown.edits, shown.text.slice(0, MAX_EDIT_LENGTH)].slice(-MAX_EDITS);
        shown.canRecordEdit = false;
    }
    return updated;
};
