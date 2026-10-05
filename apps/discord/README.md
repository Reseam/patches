# Discord

Tested on Discord 347.12 Stable, Android 16. Other versions are untested.

## Constraints

- Discord's UI and logic are JavaScript in its Hermes bundle, so most patches wrap bundle functions. Only native code stays in DEX patches: chat jumps, message swipes, crash reports and the bundle updater.
- Match functions by name, referenced strings and parameter count. Anonymous functions (`name("")`) need strings only they reference. No function IDs or bytecode offsets.
- A patch has JavaScript only when it computes something. Fixed results use `skipWhen`, `returnNullWhen`, `returnTrueWhen` or `returnFalseWhen`; a single changed option uses `setArgumentWhen`.
- Exports are named after the Discord function they wrap and take its real parameters. Call `original` directly; it is already bound to the receiver.
- Every patch has a toggle under Settings › Reseam, read once per process. Keep patched bundle has none: turning it off lets Discord's updater replace the patched bundle.
- Message history reads and writes chat messages only through Discord's own `ChannelMessages.getOrCreate` and `commit`. Kept text stays in memory, at most 1000 messages, and is cleared on logout.
- A kept deleted message ends with a zero-width space. The chat list redraws only rows whose message changed.
- Freemoji and Sticker links turn only Nitro locks into links. Role, permission and server boost locks stay.
- Hide link cards keeps Discord's own emoji and sticker image previews.
- Clean outgoing links leaves signed URLs unchanged.
- The Reseam settings row uses Discord's Advanced icon. Extension JavaScript has no handle on Discord's React to render its own.
