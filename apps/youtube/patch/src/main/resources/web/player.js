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

    const solve = input => new Promise((resolve, reject) => {
        const id = nextId++;
        pending.set(id, { resolve, reject });
        worker.postMessage({ id, input });
    });

    /** Starts the solver from ejs's lib and core scripts. */
    const start = (lib, core) => {
        const source = [lib, "\nObject.assign(globalThis, lib);\n", core, WORKER];
        worker = new Worker(URL.createObjectURL(new Blob(source, { type: "text/javascript" })));
        worker.onmessage = ({ data: { id, output, error } }) => {
            const { resolve, reject } = pending.get(id);
            pending.delete(id);
            if (error) reject(new Error(error)); else resolve(output);
        };
    };

    const fetchText = async url => {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`${url} returned HTTP ${response.status}`);
        return response.text();
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

    /** Maps each `n` challenge to its solution. */
    const solveN = async challenges => {
        if (!current) throw new Error("Player is not loaded");
        const output = await solve({
            type: "preprocessed",
            preprocessed_player: current.preprocessed,
            requests: [{ type: "n", challenges }],
        });
        const [response] = output.responses;
        if (response.type !== "result") throw new Error(response.error);
        return response.data;
    };

    return { start, load, solveN };
})();
