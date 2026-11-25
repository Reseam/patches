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

type ColorStep = [hex: string, name: string];

// Shadows write their color inside a longer string.
const HEX_COLOR = /#[0-9a-f]{6}\b/gi;

let changedPurples: Map<string, string> | undefined;
let changedNormalizedPurples: Map<number, number> | undefined;

/**
 * Gives the themes of Twitch's design tokens the app's colors. [purples] and [greys] pair each step of Twitch's purple
 * and grey scales with the name of its Android color, as `#rrggbb=name,…`; the backgrounds take the colors named
 * [darkBackgroundName] and [lightBackgroundName].
 */
export function applyTheme(
    tokens: Tokens | null | undefined,
    purples: string,
    greys: string,
    darkBackgroundName: string,
    lightBackgroundName: string,
) {
    if (tokens == null) return;
    const greySteps = colorSteps(greys);
    const colors = appColors([...greySteps.map(([, name]) => name), darkBackgroundName, lightBackgroundName]);
    const replacements = new Map([...appPurples(purples), ...changedColors(greySteps, colors)]);
    // Without Material You, or before Android 12, the app's colors keep Twitch's values.
    if (replacements.size > 0) {
        const seen = new Set<object>();
        for (const value of [tokens.darkTheme, tokens.lightTheme, tokens.overlayTheme, tokens.twitchPurple]) {
            recolor(value, replacements, seen);
        }
    }
    setBackground(tokens.darkTheme, colors[darkBackgroundName]);
    setBackground(tokens.lightTheme, colors[lightBackgroundName]);
}

/**
 * Returns [color], a `0xrrggbbaa` number from normalizeColor, with the app's color for its step of [purples] and the
 * same alpha.
 */
export function recolorNormalized(color: unknown, purples: string) {
    if (typeof color !== "number") return color;
    const replacement = appNormalizedPurples(purples).get(color >>> 8);
    return replacement == null ? color : ((replacement << 8) | (color & 0xff)) >>> 0;
}

/** Returns the steps of a `#rrggbb=name,…` list. */
function colorSteps(steps: string): ColorStep[] {
    return steps === "" ? [] : steps.split(",").map(step => step.split("=") as ColorStep);
}

/** Returns the app's colors among [names], each as `#rrggbb`, or `#rrggbbaa` when it is translucent. */
function appColors(names: string[]): Record<string, string> {
    const themeModule = NativeModules.ReseamTwitchTheme;
    // One call for several colors, because each call blocks the JavaScript thread.
    return themeModule == null ? {} : JSON.parse(themeModule.colors(names.join(",")));
}

/** Returns the steps of [purples] whose app color differs, mapped to that color. */
function appPurples(purples: string): Map<string, string> {
    if (changedPurples == null) {
        const steps = colorSteps(purples);
        changedPurples = changedColors(steps, appColors(steps.map(([, name]) => name)));
    }
    return changedPurples;
}

/** Returns the [steps] whose app color among [colors] differs from Twitch's, mapped to that app color. */
function changedColors(steps: ColorStep[], colors: Record<string, string>): Map<string, string> {
    const changed = new Map<string, string>();
    for (const [hex, name] of steps) {
        const color = colors[name];
        if (color != null && color !== hex) changed.set(hex, color);
    }
    return changed;
}

/** Returns the same steps as [appPurples], as the `0xrrggbb` numbers that a normalized color holds above its alpha. */
function appNormalizedPurples(purples: string): Map<number, number> {
    if (changedNormalizedPurples != null) return changedNormalizedPurples;
    changedNormalizedPurples = new Map();
    for (const [hex, color] of appPurples(purples)) {
        changedNormalizedPurples.set(parseInt(hex.slice(1), 16), parseInt(color.slice(1, 7), 16));
    }
    return changedNormalizedPurples;
}

function recolor(value: unknown, replacements: Map<string, string>, seen: Set<object>) {
    if (typeof value !== "object" || value === null || seen.has(value)) return;
    seen.add(value);
    const record = value as Record<string, unknown>;
    // A write to a frozen object throws in strict mode, inside the tokens module's factory.
    const writable = !Object.isFrozen(record);
    for (const key of Object.keys(record)) {
        const entry = record[key];
        if (typeof entry !== "string") {
            recolor(entry, replacements, seen);
        } else if (writable) {
            const recolored = entry.replace(HEX_COLOR, hex => replacements.get(hex.toLowerCase()) ?? hex);
            if (recolored !== entry) record[key] = recolored;
        }
    }
}

function setBackground(theme: Theme | undefined, color: string | undefined) {
    if (theme?.colors != null && color != null && !Object.isFrozen(theme.colors)) theme.colors.backgroundBody = color;
}
