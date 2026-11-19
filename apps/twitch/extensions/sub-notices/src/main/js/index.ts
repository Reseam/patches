// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type LineProps = { line?: { type?: string } | null };

// The chat line types for subs, resubs, gift subs and their upgrades, and the rewards a sub or gift shares with chat.
const subNoticeTypes = new Set([
    "subscription",
    "resub",
    "subgift",
    "communitygift",
    "giftpaidupgrade",
    "primepaidupgrade",
    "payforward",
    "extendsub",
    "subsidizedsub",
    "subsidizedsubmysterygift",
    "giftsubbasematch",
    "giftsubbonusmatch",
    "primecommunitygiftreceived",
    "rewardgift",
]);

export function isSubNotice(props: LineProps | null | undefined) {
    const type = props?.line?.type;
    return type !== undefined && subNoticeTypes.has(type);
}
