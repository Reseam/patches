// Web PoToken minter: runs YouTube's BotGuard challenge and mints tokens bound to a content ID.
// The page configuration (including EVENT_ID) and challenge must come from one response.
const botguard = (() => {
    const API = "https://www.youtube.com/api/jnn/v1/";
    const API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw";
    const REQUEST_KEY = "O43z0dpjhgX20SCx4KAo";
    const TIMEOUT_MS = 10_000;
    // The server's lifetime keeps running while Android sleeps; performance.now() may not.
    const now = () => reseamHost.elapsedRealtime();

    const post = async (endpoint, payload) => {
        return network.read(API + endpoint, {
            method: "POST",
            headers: {
                "content-type": "application/json+protobuf",
                "x-goog-api-key": API_KEY,
                "x-user-agent": "grpc-web-javascript/0.1",
            },
            body: JSON.stringify(payload),
        }, "json");
    };

    const base64 = {
        decode: text => Uint8Array.from(atob(text.replace(/-/g, "+").replace(/_/g, "/")), c => c.charCodeAt(0)),
        encodeWebSafe: bytes => btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_"),
    };

    const withTimeout = async (promise, what) => {
        let timer;
        try {
            return await Promise.race([
                promise,
                new Promise((_, reject) => {
                    timer = setTimeout(() => reject(new Error(`${what} timed out`)), TIMEOUT_MS);
                }),
            ]);
        } finally {
            clearTimeout(timer);
        }
    };

    const loadInterpreter = challenge => new Promise((resolve, reject) => {
        const element = document.createElement("script");
        const finish = error => {
            clearTimeout(timer);
            element.remove();
            if (error) reject(error); else resolve();
        };
        const timer = setTimeout(() => finish(new Error("BotGuard interpreter timed out")), TIMEOUT_MS);
        if (challenge.script) {
            element.textContent = challenge.script;
            document.head.appendChild(element);
            finish();
            return;
        }
        if (!challenge.url) {
            finish(new Error("BotGuard returned no interpreter"));
            return;
        }
        element.src = new URL(challenge.url, location.href).href;
        element.onload = () => finish();
        element.onerror = () => finish(new Error("BotGuard interpreter failed to load"));
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

    let current = null;
    let preparing = null;

    /** Creates a minter and its expiry on Android's monotonic, suspend-aware clock. */
    const create = async () => {
        const startedAt = now();
        const bootstrap = await page.load();
        globalThis.yt = { config_: bootstrap.config };
        const challenge = bootstrap.challenge;
        await loadInterpreter(challenge);
        const { response, signalOutput } = await snapshot(challenge);
        const [integrityToken, ttlSeconds] = await post("GenerateIT", [REQUEST_KEY, response]);
        if (typeof integrityToken !== "string") throw new Error("GenerateIT returned no integrity token");
        if (!Number.isFinite(ttlSeconds) || ttlSeconds <= 0) throw new Error("GenerateIT returned no valid lifetime");
        if (typeof signalOutput[0] !== "function") throw new Error("BotGuard returned no integrity callback");
        const mintCallback = await withTimeout(signalOutput[0](base64.decode(integrityToken)), "BotGuard minter");
        if (typeof mintCallback !== "function") throw new Error("BotGuard returned no minter");
        // Start the lifetime before attestation and leave room to finish a media request.
        const expiresAt = startedAt + ttlSeconds * 1000 - Math.min(30_000, ttlSeconds * 100);
        if (!Number.isFinite(expiresAt) || expiresAt <= now()) throw new Error("BotGuard session expired during preparation");
        return { minter: mintCallback, expiresAt, bootstrap, id: crypto.randomUUID() };
    };

    /** Shares attestation across concurrent requests and never retains a failed preparation. */
    const init = () => {
        if (preparing) return preparing;
        preparing = create()
            .then(session => { current = session; return session; })
            .finally(() => { preparing = null; });
        return preparing;
    };

    /** A web-safe base64 token bound to `binding`: a visitor data string, data sync ID or video ID. */
    const mint = async binding => {
        if (typeof binding !== "string" || !binding) throw new Error("Missing PoToken binding");
        const session = !current || now() >= current.expiresAt ? await init() : current;
        return mintFrom(session, binding);
    };

    const mintFrom = async (session, binding) => {
        try {
            const token = await withTimeout(session.minter(new TextEncoder().encode(binding)), "BotGuard mint");
            if (!(token instanceof Uint8Array) || !token.length) throw new Error("BotGuard minted no token");
            if (now() >= session.expiresAt) throw new Error("BotGuard session expired while minting");
            return base64.encodeWebSafe(token);
        } catch (error) {
            // Invalidate only the minter that failed. Other calls may have installed a fresh one.
            if (current === session) current = null;
            throw error;
        }
    };

    /** Returns the profile and token from the same attestation session, including after refresh. */
    const attest = async videoId => {
        const session = !current || now() >= current.expiresAt ? await init() : current;
        const { client, binding, sessionBinding } = session.bootstrap;
        const value = binding === "content" ? videoId : sessionBinding;
        const poToken = binding === "none" ? "" : await mintFrom(session, value);
        return { client, poToken, expiresAt: session.expiresAt, sessionId: session.id };
    };

    const invalidate = sessionId => {
        if (current?.id === sessionId) current = null;
    };
    return { init, mint, attest, invalidate };
})();
