// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Bulk acks reach the server only from Discord's own context; any other context updates read state
// on this phone alone.
const LOCAL_ONLY = Symbol("reseam-local-ack");

export function localAck(options: object) {
    return { ...options, local: true };
}

export function localBulkAck(action: { onFinished?: unknown }) {
    const { onFinished } = action;
    // Calling it during the ack would dispatch again inside the current Flux dispatch.
    if (typeof onFinished === "function") void Promise.resolve().then(onFinished as () => void);
    return { ...action, context: LOCAL_ONLY, onFinished: undefined };
}
