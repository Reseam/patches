// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Reads the configuration and attestation challenge from the same YouTube page response.
// Only object and string literals are decoded; page scripts are never evaluated.
const page = (() => {
    const objectCalls = (source, callee) => {
        const calls = [];
        let offset = 0;
        while ((offset = source.indexOf(callee, offset)) !== -1) {
            const start = offset;
            offset += callee.length;
            while (/\s/.test(source[offset] ?? "") && offset < source.length) offset++;
            if (source[offset++] !== "(") continue;
            while (/\s/.test(source[offset] ?? "") && offset < source.length) offset++;
            if (source[offset] !== "{") continue;
            const begin = offset;
            let depth = 0;
            let quote = null;
            let escaped = false;
            for (; offset < source.length; offset++) {
                const character = source[offset];
                if (quote) {
                    if (escaped) escaped = false;
                    else if (character === "\\") escaped = true;
                    else if (character === quote) quote = null;
                } else if (character === '"' || character === "'") quote = character;
                else if (character === "{") depth++;
                else if (character === "}" && --depth === 0) {
                    calls.push({ start, argument: source.slice(begin, ++offset) });
                    break;
                }
            }
        }
        return calls;
    };

    const stringLiteral = (source, start) => {
        const quote = source[start++];
        let result = "";
        for (let index = start; index < source.length;) {
            let character = source[index++];
            if (character === quote) return result;
            if (character !== "\\") { result += character; continue; }
            character = source[index++];
            if (character === undefined) throw new Error("Incomplete page string escape");
            if (character === "x" || character === "u") {
                const length = character === "x" ? 2 : 4;
                const hex = source.slice(index, index + length);
                if (hex.length !== length || !/^[0-9a-f]+$/i.test(hex)) throw new Error("Invalid page string escape");
                result += String.fromCharCode(parseInt(hex, 16));
                index += length;
            } else if (character === "\n") continue;
            else if (character === "\r") { if (source[index] === "\n") index++; }
            else result += ({ b: "\b", f: "\f", n: "\n", r: "\r", t: "\t", v: "\v" })[character] ?? character;
        }
        throw new Error("Unterminated page string");
    };

    /**
     * Returns the paired configuration, WEB client, token binding and BotGuard challenge.
     * Rejects when the page request fails or required configuration/challenge data is missing
     * or malformed. The page's script bodies are never executed to extract these values.
     */
    const load = async () => {
        const html = await network.read("https://www.youtube.com/");
        for (const call of objectCalls(html, "window.ytAtN")) {
            const match = /['"]R['"]\s*:\s*(['"])/.exec(call.argument);
            if (!match) continue;
            const response = JSON.parse(stringLiteral(call.argument, match.index + match[0].length - 1));
            const challenge = response.bgChallenge;
            if (!challenge?.program || !challenge.globalName) continue;
            const config = Object.assign({}, ...objectCalls(html, "ytcfg.set")
                .filter(item => item.start < call.start).map(item => JSON.parse(item.argument)));
            const client = config.INNERTUBE_CONTEXT?.client;
            if (!config.EVENT_ID || client?.clientName !== "WEB" || !client.clientVersion) {
                throw new Error("Missing paired WEB page configuration");
            }
            const visitor = config.EOM_VISITOR_DATA || config.VISITOR_DATA || client.visitorData;
            if (!visitor) throw new Error("Missing page visitor data");
            client.visitorData = visitor.replace(/%3d/ig, "=");
            const watch = config.WEB_PLAYER_CONTEXT_CONFIGS?.WEB_PLAYER_CONTEXT_CONFIG_ID_KEVLAR_WATCH;
            const flags = new URLSearchParams(watch?.serializedExperimentFlags ?? "");
            const binding = flags.get("html5_generate_content_po_token") === "true" || !watch ? "content"
                : flags.get("html5_generate_session_po_token") === "true" ? "session" : "none";
            return {
                config, client, binding,
                sessionBinding: config.DATASYNC_ID || client.visitorData,
                challenge: {
                    program: challenge.program,
                    globalName: challenge.globalName,
                    script: challenge.interpreterJavascript?.privateDoNotAccessOrElseSafeScriptWrappedValue,
                    url: challenge.interpreterUrl?.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue,
                },
            };
        }
        throw new Error("YouTube page has no attestation challenge");
    };

    let profiled = null;
    const profile = () => profiled ??= load().then(({ client, binding }) => ({ client, binding }), error => {
        profiled = null;
        throw error;
    });
    return { load, profile };
})();
