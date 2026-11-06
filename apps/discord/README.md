# Discord

Tested on Discord 347.12 Stable, Android 16. Other versions are untested.

## Constraints

- Discord's UI and logic are JavaScript in its Hermes bundle, so most patches hook bundle functions with `before`, `after` and `replace`, as DEX patches hook methods. Only native code stays in DEX patches: chat jumps, message swipes, crash reports and the bundle updater.
- Match functions by name, referenced strings and parameter count. Anonymous functions (`name("")`) need strings only they reference. No function IDs or bytecode offsets.
- A patch has JavaScript only when it computes something. Fixed results use `alwaysReturn…`; a single changed option uses `setArgument`. Hooks sit inside `gate(setting) { }`.
- Extensions are TypeScript, with an `index.ts` or `index.tsx` entry. Each export takes values the hook passes, such as `param(0)` or `result`, and returns the new value. The Kotlin binding is generated from the exports.
- Every patch has a toggle under Settings › Reseam, read once per process. Keep patched bundle has none: turning it off lets Discord's updater replace the patched bundle.
- Message history reads and writes chat messages only through Discord's own `ChannelMessages.getOrCreate` and `commit`. Kept text stays in memory, at most 1000 messages, and is cleared on logout.
- A kept deleted message ends with a zero-width space. The chat list redraws only rows whose message changed. Its row uses the colors of a message AutoMod blocked.
- Quick reply and delete starts the reply 650 ms after a double tap: the chat list reports the third tap only once its own double tap window closes. Double tapping someone else's message still reacts.
- Delete without confirmation finds the Delete option by its translated label, as Discord's own menu handler does.
- Freemoji and Sticker links turn only Nitro locks into links. Role, permission and server boost locks stay.
- Hide link cards keeps Discord's own emoji and sticker image previews.
- Clean outgoing links leaves signed URLs unchanged.
- Add source cards shows each `reseam.app/add` link as a card with the source and an add button, in place of the website preview. Messages that already have components keep them unchanged.
- The Reseam settings row uses Discord's Advanced icon.
- Extensions import Discord's modules by source path, as `discord:stores/UserStore.tsx`, and `react`, `react/jsx-runtime` and `react-native` by name. `core/Modules.kt` declares them. UI is built from Discord's own components, so it follows its theme.
- Custom themes stay on this phone and never change Discord's synced appearance settings. A theme compiles to a semantic color variant that Discord's resolver selects through the theme context's experiments. Views Discord draws natively, such as the shared theme card's button, keep Discord's colors.
