// Web PoToken minter: runs YouTube's BotGuard challenge and mints tokens bound to a content ID.
// Protocol per LuanRT/BgUtils: WAA Create -> BotGuard snapshot -> GenerateIT -> minter.
const botguard = (() => {
    const API = "https://www.youtube.com/api/jnn/v1/";
    const API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw";
    const REQUEST_KEY = "O43z0dpjhgX20SCx4KAo";
    const TIMEOUT_MS = 10_000;

    const post = async (endpoint, payload) => {
        const response = await fetch(API + endpoint, {
            method: "POST",
            headers: {
                "content-type": "application/json+protobuf",
                "x-goog-api-key": API_KEY,
                "x-user-agent": "grpc-web-javascript/0.1",
            },
            body: JSON.stringify(payload),
        });
        if (!response.ok) throw new Error(`${endpoint} returned HTTP ${response.status}`);
        return response.json();
    };

    const base64 = {
        decode: text => Uint8Array.from(atob(text.replace(/-/g, "+").replace(/_/g, "/")), c => c.charCodeAt(0)),
        encodeWebSafe: bytes => btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_"),
    };

    const withTimeout = (promise, what) => Promise.race([
        promise,
        new Promise((_, reject) => setTimeout(() => reject(new Error(`${what} timed out`)), TIMEOUT_MS)),
    ]);

    // The challenge is either inline or, for scrambled request keys, base64 with every byte shifted by 97.
    const parseChallenge = raw => {
        const fields = typeof raw[1] === "string"
            ? JSON.parse(new TextDecoder().decode(base64.decode(raw[1]).map(b => b + 97)))
            : raw[0];
        const [, script, url, , program, globalName] = fields;
        const firstString = values => Array.isArray(values) ? values.find(v => typeof v === "string" && v) : undefined;
        return { script: firstString(script), url: firstString(url), program, globalName };
    };

    const loadInterpreter = challenge => new Promise((resolve, reject) => {
        const element = document.createElement("script");
        if (challenge.script) {
            element.textContent = challenge.script;
            document.head.appendChild(element);
            resolve();
            return;
        }
        element.src = new URL(challenge.url, location.href).href;
        element.onload = resolve;
        element.onerror = () => reject(new Error("BotGuard interpreter failed to load"));
        document.head.appendChild(element);
    });

    const snapshot = async challenge => {
        const vm = globalThis[challenge.globalName];
        if (!vm?.a) throw new Error("BotGuard VM is unavailable");
        const functions = new Promise(resolve => {
            vm.a(challenge.program, (asyncSnapshot) => resolve(asyncSnapshot), true, undefined, () => {}, [[], []]);
        });
        const asyncSnapshot = await withTimeout(functions, "BotGuard load");
        const signalOutput = [];
        const response = await withTimeout(
            new Promise(resolve => asyncSnapshot(resolve, [undefined, undefined, signalOutput, undefined])),
            "BotGuard snapshot");
        return { response, signalOutput };
    };

    let minter = null;
    let expiresAt = 0;

    /** Starts a session and returns its lifetime in seconds. */
    const init = async () => {
        const challenge = parseChallenge(await post("Create", [REQUEST_KEY]));
        await loadInterpreter(challenge);
        const { response, signalOutput } = await snapshot(challenge);
        const [integrityToken, ttlSeconds] = await post("GenerateIT", [REQUEST_KEY, response]);
        if (typeof integrityToken !== "string") throw new Error("GenerateIT returned no integrity token");
        const mintCallback = await signalOutput[0](base64.decode(integrityToken));
        if (typeof mintCallback !== "function") throw new Error("BotGuard returned no minter");
        minter = mintCallback;
        expiresAt = Date.now() + ttlSeconds * 1000;
        return ttlSeconds;
    };

    /** A web-safe base64 token bound to `binding`: a visitor data string, data sync ID or video ID. */
    const mint = async binding => {
        if (!minter || Date.now() >= expiresAt) await init();
        const token = await minter(new TextEncoder().encode(binding));
        if (!(token instanceof Uint8Array)) throw new Error("BotGuard minted no token");
        return base64.encodeWebSafe(token);
    };

    return { init, mint };
})();
