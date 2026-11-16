// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Every copy shares one empty list, so the strip's memoized pages see the same boards.
const noBoards: never[] = [];

// Each render reads the same context value, so this keeps one copy per value.
const copies = new WeakMap<object, object>();

// Without boards or a top supporter, the strip above chat keeps only its goal pages, and draws nothing when there are none.
export function withoutLeaderboards(leaderboard: object | null | undefined) {
    if (leaderboard == null) return leaderboard;
    let copy = copies.get(leaderboard);
    if (copy === undefined) {
        copy = { ...leaderboard, boards: noBoards, hasLeaderboard: false, topSupporter: null };
        copies.set(leaderboard, copy);
    }
    return copy;
}
