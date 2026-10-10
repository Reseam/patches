// The current web player and its stream URL challenges, solved with yt-dlp's ejs solver (`jsc`).
// The solver runs the player's own code, which can navigate the page, so it runs in a worker.
const player = (() => {
    const ORIGIN = "https://www.youtube.com";
    const WORKER = `
        onmessage = ({ data: { id, input } }) => {
            try {
                const output = jsc(input);
                if (output.type !== "result") throw new Error(output.error);
                postMessage({ id, output });
            } catch (error) {
                postMessage({ id, error: String(error) });
            }
        };`;

    let worker = null;
    let current = null;
    let nextId = 0;
    const pending = new Map();

    const fail = error => {
        worker?.terminate();
        worker = null;
        for (const { reject, timer } of pending.values()) {
            clearTimeout(timer);
            reject(error);
        }
        pending.clear();
        reseamHost.fail(String(error));
    };

    const solve = input => new Promise((resolve, reject) => {
        const id = nextId++;
        const timer = setTimeout(() => fail(new Error("Player solver timed out")), 10_000);
        pending.set(id, { resolve, reject, timer });
        try {
            worker.postMessage({ id, input });
        } catch (error) {
            fail(error);
        }
    });

    /** Starts the solver from ejs's lib and core scripts. */
    const start = (lib, core) => {
        const source = [lib, "\nObject.assign(globalThis, lib);\n", core, WORKER];
        const url = URL.createObjectURL(new Blob(source, { type: "text/javascript" }));
        try {
            worker = new Worker(url);
        } finally {
            URL.revokeObjectURL(url);
        }
        worker.onmessage = ({ data: { id, output, error } }) => {
            const call = pending.get(id);
            if (!call) return;
            const { resolve, reject, timer } = call;
            pending.delete(id);
            clearTimeout(timer);
            if (error) reject(new Error(error)); else resolve(output);
        };
        worker.onerror = event => {
            event.preventDefault();
            fail(new Error("Player solver worker failed"));
        };
        worker.onmessageerror = () => fail(new Error("Player solver returned an unreadable message"));
    };

    const fetchText = async url => {
        return network.read(url);
    };

    /** Loads the player the web client currently serves; returns its signature timestamp. */
    const load = async () => {
        const id = (await fetchText(`${ORIGIN}/iframe_api`)).match(/player\\?\/([0-9a-fA-F]{8})\\?\//)?.[1];
        if (!id) throw new Error("No player ID in iframe_api");
        if (current?.id === id) return current.signatureTimestamp;
        const code = await fetchText(`${ORIGIN}/s/player/${id}/player_ias.vflset/en_US/base.js`);
        const signatureTimestamp = Number(code.match(/(?:signatureTimestamp|sts)\s*:\s*(\d{5})/)?.[1]);
        if (!signatureTimestamp) throw new Error(`No signature timestamp in player ${id}`);
        const output = await solve({ type: "player", player: code, requests: [], output_preprocessed: true });
        current = { id, signatureTimestamp, preprocessed: output.preprocessed_player };
        return signatureTimestamp;
    };

    /** Solves stream URL challenges, given as lists by type (`n`, `sig`); maps each to its solution by type. */
    const solveChallenges = async challenges => {
        if (!current) throw new Error("Player is not loaded");
        const requests = Object.entries(challenges)
            .filter(([, values]) => values.length)
            .map(([type, values]) => ({ type, challenges: values }));
        if (!requests.length) return {};
        const output = await solve({ type: "preprocessed", preprocessed_player: current.preprocessed, requests });
        return Object.fromEntries(output.responses.map((response, index) => {
            if (response.type !== "result") throw new Error(response.error);
            return [requests[index].type, response.data];
        }));
    };

    return { start, load, solveChallenges };
})();
