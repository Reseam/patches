// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Parameters each app adds to its share links. YouTube keeps t, its timestamp.
const shareParameters = new Map([
    [["youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"], ["si", "feature"]],
    [["reddit.com", "www.reddit.com", "old.reddit.com"], ["share_id"]],
    [["instagram.com", "www.instagram.com"], ["igsh", "igshid"]],
    [["twitter.com", "www.twitter.com", "x.com", "www.x.com"], ["s", "t"]],
].flatMap(([hosts, names]) => hosts.map(host => [host, new Set(names)])));
const adParameters = new Set(["gclid", "fbclid"]);

function isTracking(host, name) {
    return name !== null && (name.startsWith("utm_") || adParameters.has(name) || shareParameters.get(host).has(name));
}

function parameterName(pair) {
    try {
        return decodeURIComponent(pair.split("=", 1)[0]).toLowerCase();
    } catch (_) {
        return null;
    }
}

function isSigningParameter(name) {
    return name === "signature" || name === "sig" || name === "token" || name.startsWith("x-amz-");
}

function clean(url) {
    const match = /^(https?:\/\/)([^/?#]+)([^?#]*)(?:\?([^#]*))?(#.*)?$/i.exec(url);
    if (!match || !shareParameters.has(match[2].toLowerCase()) || match[4] === undefined) return url;
    const [, scheme, host, path, query, fragment = ""] = match;
    const pairs = query.split("&");
    const names = pairs.map(parameterName);
    // A signed URL stops working when its query changes.
    if (names.some(name => name !== null && isSigningParameter(name))) return url;
    const kept = pairs.filter((pair, index) => !isTracking(host.toLowerCase(), names[index]));
    if (kept.length === pairs.length) return url;
    return `${scheme}${host}${path}${kept.length ? `?${kept.join("&")}` : ""}${fragment}`;
}

function count(text, character) {
    return text.split(character).length - 1;
}

// Punctuation and unbalanced closing parentheses at the end belong to the sentence around the link.
function withoutTrailing(token) {
    let url = token;
    while (/[.,!?;]$/.test(url) || (url.endsWith(")") && count(url, ")") > count(url, "("))) {
        url = url.slice(0, -1);
    }
    return url;
}

exports.sendMessage = function (original, channelId, message, options) {
    if (typeof message.content !== "string") return original(channelId, message, options);
    const content = message.content.replace(/https?:\/\/[^\s<>`"']+/gi, token => {
        const url = withoutTrailing(token);
        return clean(url) + token.slice(url.length);
    });
    return original(channelId, { ...message, content }, options);
};
