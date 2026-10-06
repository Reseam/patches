// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Bulk acks reach the server only from Discord's own context; any other context
// updates read state on this phone alone.
const localOnly = Symbol("reseam-local-ack");

exports.ack = function (original, options) {
    return original({ ...options, local: true });
};

exports.handleBulkAck = function (original, action) {
    const result = original({ ...action, context: localOnly, onFinished: undefined });
    const onFinished = action.onFinished;
    // Calling it now would dispatch again inside the current Flux dispatch.
    if (typeof onFinished === "function") Promise.resolve().then(onFinished);
    return result;
};
