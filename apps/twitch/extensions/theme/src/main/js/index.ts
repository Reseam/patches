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

// Shadows write their color inside a longer string.
const HEX_COLOR = /#[0-9a-f]{6}\b/gi;

/**
 * Gives the themes of Twitch's design tokens the app's colors. [purples] pairs each of Twitch's purples with the
 * name of its Android color, as `#rrggbb=name,…`; the backgrounds take the colors named [darkBackgroundName] and
 * [lightBackgroundName].
 */
export function applyTheme(tokens: Tokens | null | undefined, purples: string, darkBackgroundName: string, lightBackgroundName: string) {
    const themeModule = NativeModules.ReseamTwitchTheme;
    if (tokens == null || themeModule == null) return;
    const steps = purples === "" ? [] : purples.split(",").map(step => step.split("=") as [string, string]);
    // One call for every color, because each call blocks the JavaScript thread.
    const names = [...steps.map(([, name]) => name), darkBackgroundName, lightBackgroundName];
    const colors: Record<string, string> = JSON.parse(themeModule.colors(names.join(",")));
    const recolored = new Map<string, string>();
    for (const [hex, name] of steps) {
        const color = colors[name];
        if (color != null && color !== hex) recolored.set(hex, color);
    }
    // Before Android 12 the app's purple colors keep Twitch's values.
    if (recolored.size > 0) {
        const seen = new Set<object>();
        for (const value of [tokens.darkTheme, tokens.lightTheme, tokens.overlayTheme, tokens.twitchPurple]) {
            recolor(value, recolored, seen);
        }
    }
    setBackground(tokens.darkTheme, colors[darkBackgroundName]);
    setBackground(tokens.lightTheme, colors[lightBackgroundName]);
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
            const replaced = entry.replace(HEX_COLOR, hex => purples.get(hex.toLowerCase()) ?? hex);
            if (replaced !== entry) record[key] = replaced;
        }
    }
}

function setBackground(theme: Theme | undefined, color: string | undefined) {
    if (theme?.colors != null && color != null && !Object.isFrozen(theme.colors)) theme.colors.backgroundBody = color;
}
