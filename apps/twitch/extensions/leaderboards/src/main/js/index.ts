// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// The strip memoizes its pages by the boards list.
const noBoards: never[] = [];

const copies = new WeakMap<object, object>();

// Without boards or a top supporter, the strip keeps only its goal pages.
export function withoutLeaderboards(leaderboard: object | null | undefined) {
    if (leaderboard == null) return leaderboard;
    let copy = copies.get(leaderboard);
    if (copy === undefined) {
        copy = { ...leaderboard, boards: noBoards, hasLeaderboard: false, topSupporter: null };
        copies.set(leaderboard, copy);
    }
    return copy;
}
