// Bound network operations, including response bodies, to the runtime's call lifetime.
const network = {
    async read(url, options = {}, json = false) {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), 10_000);
        try {
            const response = await fetch(url, { ...options, signal: controller.signal, cache: "no-store" });
            if (!response.ok) throw new Error(`Web request returned HTTP ${response.status}`);
            return await (json ? response.json() : response.text());
        } finally {
            clearTimeout(timer);
        }
    },
};

// Entry points for the app, which calls them through `host.call` and hears back on `reseamHost`.
const web = {
    /** Loads the player and a BotGuard session; returns the player's signature timestamp. */
    prepare: async () => (await Promise.all([player.load(), botguard.init()]))[0],

    attest: videoId => botguard.attest(videoId),
    invalidateAttestation: sessionId => botguard.invalidate(sessionId),
    solve: challenges => player.solveChallenges(challenges),

    /** A PoToken bound to `binding` and the solution of each stream URL challenge, by type. */
    unlock: async (binding, challenges) => {
        const [poToken, solved] = await Promise.all([botguard.mint(binding), player.solveChallenges(challenges)]);
        return { poToken, n: solved.n ?? {}, sig: solved.sig ?? {} };
    },
};

const host = {
    call(id, task, args) {
        Promise.resolve()
            .then(() => task(...args))
            .then(result => reseamHost.resolve(id, JSON.stringify(result ?? null)),
                error => reseamHost.reject(id, String(error?.stack ?? error)));
    },
};

(async () => {
    const [lib, core] = await Promise.all([
        network.read("ejs/yt.solver.lib.min.js"), network.read("ejs/yt.solver.core.min.js"),
    ]);
    player.start(lib, core);
    reseamHost.ready();
})().catch(error => reseamHost.fail(String(error?.stack ?? error)));
