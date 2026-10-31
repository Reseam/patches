// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { argb, type Color, formatColor, opaqueHex, paletteAt, parseColor, toneOf, withAlpha } from "./color";
import { PALETTE_FAMILIES, type PaletteKey, paletteKeyOf, type Theme, type ThemeBase, type ThemeEntry } from "./theme";

// A theme compiles to a semantic color variant, the kind Discord's resolver picks by the theme
// context's enabledExperiments: experiments[token][variant][theme] = { raw, opacity }.

interface SemanticColor {
    raw: string;
    opacity: number;
}

type SemanticDefinitions = Record<string, Partial<Record<ThemeBase, SemanticColor>>>;

/** Discord's color tables, as its token modules export them. */
interface Tables {
    /** The raw colors Discord reads, which an applied theme overwrites. */
    raw: Record<string, string>;
    pristineRaw: Record<string, string>;
    semantic: SemanticDefinitions;
    experiments: Record<string, Record<string, Partial<Record<ThemeBase, SemanticColor>>>>;
}

const tables: Partial<Tables> = {};
const compiledVariants = new Map<string, Record<string, Color>>();
const swatches = new WeakMap<Theme, string[]>();
const CUSTOM_GRADIENT_ENTRY = "customBackgroundGradient";

const table = <Key extends keyof Tables>(key: Key): Tables[Key] => {
    const value = tables[key];
    if (value === undefined) throw new Error(`Discord's color ${key} table has not loaded`);
    return value;
};

export function captureTokens(tokens: { RawColor: Record<string, string>; SemanticColor: SemanticDefinitions }) {
    tables.raw = tokens.RawColor;
    tables.pristineRaw = { ...tokens.RawColor };
    tables.semantic = tokens.SemanticColor;
}

export function captureExperiments(definitions: { _private: { SemanticColorExperiments: Tables["experiments"] } }) {
    tables.experiments = definitions._private.SemanticColorExperiments;
}

export const semanticTokens = () => table("semantic");

function rawColorOf(theme: Theme, name: string): Color {
    const themed = theme.raw[name];
    if (themed !== undefined) return themed;
    const color = parseColor(table("pristineRaw")[name]!);
    const key = paletteKeyOf(name);
    const palette = key === null ? undefined : theme.palettes[key];
    return palette === undefined ? color : paletteAt(palette, toneOf(color), color.a);
}

function compileTheme(theme: Theme): Record<string, Color> {
    const colors: Record<string, Color> = {};
    for (const [token, definition] of Object.entries(table("semantic"))) {
        const value = definition[theme.base];
        if (value !== undefined) colors[token] = withAlpha(rawColorOf(theme, value.raw), value.opacity);
    }
    return { ...colors, ...theme.semantic };
}

export const variantName = (entry: ThemeEntry) => `reseam-theme/${entry.id}/${entry.revision}`;

/** The variant Discord's resolver uses for [entry], compiled into its tables on first use. */
export function themeVariant(entry: ThemeEntry): string {
    const name = variantName(entry);
    if (compiledVariants.has(name)) return name;
    const colors = compileTheme(entry.theme);
    const experiments = table("experiments");
    for (const [token, color] of Object.entries(colors)) {
        if (!(token in table("semantic"))) continue;
        const raw = `RESEAM_${opaqueHex(color).slice(1)}`;
        table("raw")[raw] = opaqueHex(color);
        (experiments[token] ??= {})[name] = { [entry.theme.base]: { raw, opacity: color.a } };
    }
    compiledVariants.set(name, colors);
    return name;
}

export const compiledColors = (entry: ThemeEntry) => compiledVariants.get(themeVariant(entry))!;

// Code that read a raw color while it loaded keeps it, so startup sets them first.
export function setRawColors(theme: Theme | null) {
    const raw = table("raw");
    for (const [name, color] of Object.entries(table("pristineRaw"))) {
        const key = paletteKeyOf(name);
        const themed = theme !== null && (name in theme.raw || (key !== null && key in theme.palettes));
        raw[name] = themed ? formatColor(rawColorOf(theme, name)) : color;
    }
}

/** Two colors that stand for [theme] in lists: its background, or its base and brand colors. */
export function themeSwatch(theme: Theme): string[] {
    if (theme.background !== null) return theme.background.colors.map(opaqueHex);
    let swatch = swatches.get(theme);
    if (swatch === undefined) {
        const colors = compileTheme(theme);
        swatch = [opaqueHex(colors.BACKGROUND_BASE_LOW!), opaqueHex(colors.BACKGROUND_BRAND!)];
        swatches.set(theme, swatch);
    }
    return swatch;
}

/** The colors Discord's native views draw with, as Android ARGB. */
export function nativeColors(entry: ThemeEntry) {
    const colors = Object.entries(compiledColors(entry)).map(([token, color]) => [token, argb(color)]);
    return { base: entry.theme.base, colors: Object.fromEntries(colors) };
}

/** [entry]'s background as Discord's custom gradient, in its colors or only the first. */
export function themeBackground(entry: ThemeEntry, gradient: boolean) {
    const background = entry.theme.background;
    if (background === null) return null;
    const colors = gradient ? background.colors : [background.colors[0]!, background.colors[0]!];
    return {
        type: CUSTOM_GRADIENT_ENTRY,
        theme: entry.theme.base,
        getName: () => entry.theme.name,
        customThemeSettings: {
            colors: colors.map(opaqueHex),
            gradientColorStops: [],
            gradientAngle: background.angle,
            baseMix: background.mix,
        },
    };
}

export type ThemeBackground = NonNullable<ReturnType<typeof themeBackground>>;

/** Discord's own tone 50 of a palette, for the palette editor. */
export const discordPaletteColor = (key: PaletteKey) =>
    withAlpha(parseColor(table("pristineRaw")[`${PALETTE_FAMILIES[key][0]}_50`]!), 1);
