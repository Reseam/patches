// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { NativeModules } from "react-native";

type Theme = { colors?: Record<string, unknown> };

type Tokens = {
    darkTheme?: Theme;
    lightTheme?: Theme;
    overlayTheme?: Theme;
    twitchPurple?: Record<string, string>;
};

// Twitch's purple scale as its design tokens write it, and the Android color of the same step.
// Step 1 has no Android color.
const PURPLES: Record<string, string> = {
    "#0d031c": "twitch_purple_2",
    "#15052e": "twitch_purple_3",
    "#23094e": "twitch_purple_4",
    "#330c6e": "twitch_purple_5",
    "#451093": "twitch_purple_6",
    "#5c16c5": "twitch_purple_7",
    "#772ce8": "twitch_purple_8",
    "#9147ff": "twitch_purple_9",
    "#a970ff": "twitch_purple_10",
    "#bf94ff": "twitch_purple_11",
    "#d1b3ff": "twitch_purple_12",
    "#e3d1ff": "twitch_purple_13",
    "#ede0ff": "twitch_purple_14",
    "#f3ebff": "twitch_purple_15",
};

// Shadows write their color inside a longer string.
const HEX_COLOR = /#[0-9a-f]{6}\b/gi;

let changedPurples: Map<string, string> | undefined;
let changedNormalizedPurples: Map<number, number> | undefined;

/** Gives the themes of Twitch's design tokens the app's purple colors, and the background colors named [darkBackgroundName] and [lightBackgroundName]. */
export function applyTheme(tokens: Tokens | null | undefined, darkBackgroundName: string, lightBackgroundName: string) {
    if (tokens == null) return;
    const purples = appPurples();
    // Without Material You, or before Android 12, the app's purple colors keep Twitch's values.
    if (purples.size > 0) {
        const seen = new Set<object>();
        for (const value of [tokens.darkTheme, tokens.lightTheme, tokens.overlayTheme, tokens.twitchPurple]) {
            recolor(value, purples, seen);
        }
    }
    const backgrounds = appColors([darkBackgroundName, lightBackgroundName]);
    setBackground(tokens.darkTheme, backgrounds[darkBackgroundName]);
    setBackground(tokens.lightTheme, backgrounds[lightBackgroundName]);
}

/** Returns [color], a `0xrrggbbaa` number from normalizeColor, with the app's color for its purple step and the same alpha. */
export function recolorNormalized(color: unknown) {
    if (typeof color !== "number") return color;
    const replacement = appNormalizedPurples().get(color >>> 8);
    return replacement == null ? color : ((replacement << 8) | (color & 0xff)) >>> 0;
}

/** Returns the app's colors among [names], each as `#rrggbb`, or `#rrggbbaa` when it is translucent. */
function appColors(names: string[]): Record<string, string> {
    const themeModule = NativeModules.ReseamTwitchTheme;
    // One call for several colors, because each call blocks the JavaScript thread.
    return themeModule == null ? {} : JSON.parse(themeModule.colors(names.join(",")));
}

/** Returns the steps of Twitch's purple scale whose app color differs, mapped to that color. */
function appPurples(): Map<string, string> {
    if (changedPurples != null) return changedPurples;
    const colors = appColors(Object.values(PURPLES));
    changedPurples = new Map();
    for (const [hex, name] of Object.entries(PURPLES)) {
        const color = colors[name];
        if (color != null && color !== hex) changedPurples.set(hex, color);
    }
    return changedPurples;
}

/** Returns the same steps as [appPurples], as the `0xrrggbb` numbers that a normalized color holds above its alpha. */
function appNormalizedPurples(): Map<number, number> {
    if (changedNormalizedPurples != null) return changedNormalizedPurples;
    changedNormalizedPurples = new Map();
    for (const [hex, color] of appPurples()) {
        changedNormalizedPurples.set(parseInt(hex.slice(1), 16), parseInt(color.slice(1, 7), 16));
    }
    return changedNormalizedPurples;
}

function recolor(value: unknown, purples: Map<string, string>, seen: Set<object>) {
    if (typeof value !== "object" || value === null || seen.has(value)) return;
    seen.add(value);
    const record = value as Record<string, unknown>;
    // A write to a frozen object throws in strict mode, inside the tokens module's factory.
    const writable = !Object.isFrozen(record);
    for (const key of Object.keys(record)) {
        const entry = record[key];
        if (typeof entry !== "string") {
            recolor(entry, purples, seen);
        } else if (writable) {
            const recolored = entry.replace(HEX_COLOR, hex => purples.get(hex.toLowerCase()) ?? hex);
            if (recolored !== entry) record[key] = recolored;
        }
    }
}

function setBackground(theme: Theme | undefined, color: string | undefined) {
    if (theme?.colors != null && color != null && !Object.isFrozen(theme.colors)) theme.colors.backgroundBody = color;
}
