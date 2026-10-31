// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { type Color, formatColor, opaqueHex, type Palette, parseColor, withAlpha } from "./color";

// A semantic color wins over the raw color its token uses, a raw color over the palette for its
// family.

const THEME_FORMAT = "reseam.discord-theme";
const THEME_VERSION = 1;
export const THEME_FILE_SUFFIX = ".reseam-theme.json";

export const THEME_BASES = ["darker", "midnight", "light"] as const;
export type ThemeBase = (typeof THEME_BASES)[number];

export const PALETTE_FAMILIES = {
    neutral: ["NEUTRAL", "PRIMARY", "PLUM", "OPACITY"],
    brand: ["BLURPLE", "BRAND", "OPACITY_BLURPLE"],
    red: ["RED_NEW", "RED", "OPACITY_RED"],
    green: ["GREEN_NEW", "GREEN", "OPACITY_GREEN"],
    yellow: ["YELLOW_NEW", "YELLOW", "OPACITY_YELLOW"],
    blue: ["BLUE_NEW", "BLUE", "OPACITY_BLUE"],
} as const;
export type PaletteKey = keyof typeof PALETTE_FAMILIES;
export const PALETTE_KEYS = Object.keys(PALETTE_FAMILIES) as PaletteKey[];

export const MAX_BACKGROUND_COLORS = 5;
export const DEFAULT_BACKGROUND_MIX = 74;

const COLOR_NAME = /^[A-Z][A-Z0-9_]*$/;

export interface Background {
    colors: Color[];
    angle: number;
    mix: number;
}

export interface Theme {
    name: string;
    author: string | null;
    base: ThemeBase;
    palettes: Partial<Record<PaletteKey, Palette>>;
    raw: Record<string, Color>;
    semantic: Record<string, Color>;
    background: Background | null;
}

/** A theme file, as JSON holds it. */
export interface ThemeDocument {
    format: string;
    version: number;
    name: string;
    author?: string;
    base: string;
    palettes?: Record<string, Record<string, string>>;
    raw?: Record<string, string>;
    semantic?: Record<string, string>;
    background?: { colors: string[]; angle?: number; mix?: number } | null;
}

function requireText(value: unknown, field: string): string {
    if (typeof value !== "string") throw new Error(`The theme's ${field} is not text`);
    return value;
}

const isObject = (value: unknown): value is Record<string, unknown> => value !== null && typeof value === "object";

function parseColors(value: unknown, field: string): Record<string, Color> {
    if (value === undefined) return {};
    if (!isObject(value)) throw new Error(`The theme's ${field} colors are not an object`);
    return Object.fromEntries(
        Object.entries(value).map(([name, color]) => {
            if (!COLOR_NAME.test(name)) throw new Error(`${name} is not a Discord color name`);
            return [name, parseColor(requireText(color, `${field} color ${name}`))];
        }),
    );
}

const isPaletteKey = (key: string): key is PaletteKey => key in PALETTE_FAMILIES;

function parsePalette(key: string, value: unknown): [PaletteKey, Palette] {
    if (!isPaletteKey(key)) throw new Error(`${key} is not a palette`);
    if (!isObject(value)) throw new Error(`The ${key} palette is not an object`);
    const stops = Object.entries(value)
        .map(([tone, color]): [number, Color] => {
            const number = Number(tone);
            if (!Number.isFinite(number) || number < 0 || number > 100) {
                throw new Error(`${tone} is not a tone from 0 to 100`);
            }
            return [number, parseColor(requireText(color, `${key} palette color`))];
        })
        .sort(([a], [b]) => a - b);
    if (stops.length === 0) throw new Error(`The ${key} palette has no colors`);
    return [key, stops];
}

function parseBackground(value: unknown): Background | null {
    if (value === undefined || value === null) return null;
    if (!isObject(value) || !Array.isArray(value.colors)) throw new Error("The theme's background has no colors");
    if (value.colors.length === 0 || value.colors.length > MAX_BACKGROUND_COLORS) {
        throw new Error(`A background has 1 to ${MAX_BACKGROUND_COLORS} colors`);
    }
    const angle = value.angle ?? 0;
    const mix = value.mix ?? DEFAULT_BACKGROUND_MIX;
    if (typeof angle !== "number" || angle < 0 || angle > 360) throw new Error(`${angle} is not an angle from 0 to 360`);
    if (typeof mix !== "number" || mix < 0 || mix > 100) throw new Error(`${mix} is not a background strength from 0 to 100`);
    return {
        colors: value.colors.map(color => withAlpha(parseColor(requireText(color, "background color")), 1)),
        angle,
        mix,
    };
}

const isBase = (base: unknown): base is ThemeBase => THEME_BASES.includes(base as ThemeBase);

export function parseTheme(document: unknown): Theme {
    if (!isObject(document)) throw new Error("A theme file holds a JSON object");
    if (document.format !== THEME_FORMAT) throw new Error("This is not a Reseam Discord theme");
    if (document.version !== THEME_VERSION) throw new Error(`Theme version ${document.version} is not supported`);
    const name = requireText(document.name, "name").trim();
    if (name === "") throw new Error("The theme has no name");
    if (!isBase(document.base)) throw new Error(`${document.base} is not a base theme`);
    const palettes = document.palettes ?? {};
    if (!isObject(palettes)) throw new Error("The theme's palettes are not an object");
    return {
        name,
        author: document.author === undefined ? null : requireText(document.author, "author"),
        base: document.base,
        palettes: Object.fromEntries(Object.entries(palettes).map(([key, stops]) => parsePalette(key, stops))),
        raw: parseColors(document.raw, "raw"),
        semantic: parseColors(document.semantic, "semantic"),
        background: parseBackground(document.background),
    };
}

const serializeColors = (colors: Record<string, Color>) =>
    Object.fromEntries(Object.entries(colors).map(([name, color]) => [name, formatColor(color)]));

export function serializeTheme(theme: Theme): ThemeDocument {
    return {
        format: THEME_FORMAT,
        version: THEME_VERSION,
        name: theme.name,
        ...(theme.author === null ? {} : { author: theme.author }),
        base: theme.base,
        palettes: Object.fromEntries(
            Object.entries(theme.palettes).map(([key, stops]) => [
                key,
                Object.fromEntries(stops.map(([tone, color]) => [String(tone), formatColor(color)])),
            ]),
        ),
        raw: serializeColors(theme.raw),
        semantic: serializeColors(theme.semantic),
        ...(theme.background === null
            ? {}
            : {
                  background: {
                      colors: theme.background.colors.map(opaqueHex),
                      angle: theme.background.angle,
                      mix: theme.background.mix,
                  },
              }),
    };
}

/** The palette a raw color like `NEUTRAL_92` belongs to. */
export function paletteKeyOf(rawName: string): PaletteKey | null {
    const family = rawName.replace(/_\d+$/, "");
    return PALETTE_KEYS.find(key => (PALETTE_FAMILIES[key] as readonly string[]).includes(family)) ?? null;
}

export function themeFileName(theme: Theme): string {
    const name =
        theme.name
            .replace(/[\\/:*?"<>|.]+/g, "")
            .trim()
            .replace(/\s+/g, "-") || "theme";
    return `${name}${THEME_FILE_SUFFIX}`;
}

/** A theme in the library: built in, saved on this phone, or shared in a chat. */
export interface ThemeEntry {
    id: string;
    revision: number;
    builtIn: boolean;
    theme: Theme;
}
