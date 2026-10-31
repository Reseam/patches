// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Parameters each app adds to its share links. YouTube keeps t, its timestamp.
const SHARE_PARAMETERS = new Map(
    (
        [
            [["youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"], ["si", "feature"]],
            [["reddit.com", "www.reddit.com", "old.reddit.com"], ["share_id"]],
            [["instagram.com", "www.instagram.com"], ["igsh", "igshid"]],
            [["twitter.com", "www.twitter.com", "x.com", "www.x.com"], ["s", "t"]],
        ] satisfies [string[], string[]][]
    ).flatMap(([hosts, names]) => hosts.map(host => [host, new Set(names)] as const)),
);
const AD_PARAMETERS = new Set(["gclid", "fbclid"]);
const LINK = /^(https?:\/\/)([^/?#]+)([^?#]*)(?:\?([^#]*))?(#.*)?$/i;

function parameterName(pair: string): string | null {
    try {
        return decodeURIComponent(pair.split("=", 1)[0] ?? "").toLowerCase();
    } catch {
        return null;
    }
}

const isSigningParameter = (name: string) =>
    name === "signature" || name === "sig" || name === "token" || name.startsWith("x-amz-");

function clean(url: string): string {
    const [, scheme, host, path, query, fragment = ""] = LINK.exec(url) ?? [];
    const tracked = host === undefined ? undefined : SHARE_PARAMETERS.get(host.toLowerCase());
    if (tracked === undefined || query === undefined) return url;
    const pairs = query.split("&");
    const names = pairs.map(parameterName);
    // A signed URL stops working when its query changes.
    if (names.some(name => name !== null && isSigningParameter(name))) return url;
    const kept = pairs.filter((_, index) => {
        const name = names[index];
        return name == null || !(name.startsWith("utm_") || AD_PARAMETERS.has(name) || tracked.has(name));
    });
    if (kept.length === pairs.length) return url;
    return `${scheme}${host}${path}${kept.length ? `?${kept.join("&")}` : ""}${fragment}`;
}

const count = (text: string, character: string) => text.split(character).length - 1;

// Punctuation and unbalanced closing parentheses at the end belong to the sentence around the link.
function withoutTrailing(token: string): string {
    let url = token;
    while (/[.,!?;]$/.test(url) || (url.endsWith(")") && count(url, ")") > count(url, "("))) {
        url = url.slice(0, -1);
    }
    return url;
}

export function cleanLinks<Message extends { content?: unknown }>(message: Message): Message {
    if (typeof message.content !== "string") return message;
    const content = message.content.replace(/https?:\/\/[^\s<>`"']+/gi, token => {
        const url = withoutTrailing(token);
        return clean(url) + token.slice(url.length);
    });
    return { ...message, content };
}
