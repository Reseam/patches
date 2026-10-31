// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

/** A color with channels from 0 to 1. */
export interface Color {
    r: number;
    g: number;
    b: number;
    a: number;
}

/** Colors by tone from 0 (black) to 100 (white), sorted by tone. */
export type Palette = [tone: number, color: Color][];

interface Oklab {
    L: number;
    A: number;
    B: number;
}

const PALETTE_TONES = [0, 4, 6, 10, 12, 17, 20, 22, 24, 30, 40, 50, 60, 70, 80, 87, 90, 92, 94, 95, 96, 98, 99, 100];
const HEX_COLOR = /^#([0-9a-f]{6})([0-9a-f]{2})?$/i;

export function parseColor(text: string): Color {
    const [, rgb, alpha] = HEX_COLOR.exec(text) ?? [];
    if (rgb === undefined) throw new Error(`${text} is not a #RRGGBB or #RRGGBBAA color`);
    return fromRgbInt(parseInt(rgb, 16), alpha === undefined ? 1 : parseInt(alpha, 16) / 255);
}

const hexByte = (channel: number) =>
    Math.round(Math.min(1, Math.max(0, channel)) * 255)
        .toString(16)
        .padStart(2, "0");

export const opaqueHex = (color: Color) => `#${hexByte(color.r)}${hexByte(color.g)}${hexByte(color.b)}`.toUpperCase();

export function formatColor(color: Color): string {
    return Math.round(color.a * 255) === 255 ? opaqueHex(color) : `${opaqueHex(color)}${hexByte(color.a).toUpperCase()}`;
}

export const withAlpha = (color: Color, a: number): Color => ({ ...color, a });

export function argb(color: Color): number {
    const byte = (channel: number) => Math.round(Math.min(1, Math.max(0, channel)) * 255);
    return ((byte(color.a) << 24) | (byte(color.r) << 16) | (byte(color.g) << 8) | byte(color.b)) >>> 0;
}

export const fromRgbInt = (value: number, a: number): Color => ({
    r: ((value >> 16) & 255) / 255,
    g: ((value >> 8) & 255) / 255,
    b: (value & 255) / 255,
    a,
});

export const rgbInt = (color: Color) => argb(color) & 0xffffff;

const linear = (channel: number) => (channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4);

const gamma = (channel: number) => (channel <= 0.0031308 ? channel * 12.92 : 1.055 * channel ** (1 / 2.4) - 0.055);

/** CIELAB lightness, the tone palettes are indexed by. */
export function toneOf(color: Color): number {
    const y = 0.2126 * linear(color.r) + 0.7152 * linear(color.g) + 0.0722 * linear(color.b);
    return y > 216 / 24389 ? 116 * Math.cbrt(y) - 16 : (y * 24389) / 27;
}

function toOklab(color: Color): Oklab {
    const [r, g, b] = [linear(color.r), linear(color.g), linear(color.b)];
    const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
    const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
    const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
    return {
        L: 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
        A: 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
        B: 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
    };
}

function oklabToLinear(lab: Oklab): [number, number, number] {
    const l = (lab.L + 0.3963377774 * lab.A + 0.2158037573 * lab.B) ** 3;
    const m = (lab.L - 0.1055613458 * lab.A - 0.0638541728 * lab.B) ** 3;
    const s = (lab.L - 0.0894841775 * lab.A - 1.291485548 * lab.B) ** 3;
    return [
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
    ];
}

function fromOklab(lab: Oklab, a: number): Color {
    const [r, g, b] = oklabToLinear(lab).map(channel => gamma(Math.min(1, Math.max(0, channel)))) as [
        number,
        number,
        number,
    ];
    return { r, g, b, a };
}

const inGamut = (lab: Oklab) => oklabToLinear(lab).every(channel => channel >= -1e-4 && channel <= 1 + 1e-4);

function mixOklab(from: Color, to: Color, amount: number): Color {
    const [a, b] = [toOklab(from), toOklab(to)];
    return fromOklab({ L: a.L + (b.L - a.L) * amount, A: a.A + (b.A - a.A) * amount, B: a.B + (b.B - a.B) * amount }, 1);
}

/** The palette's color at [tone], mixed between the stops around it. */
export function paletteAt(palette: Palette, tone: number, a: number): Color {
    const upper = palette.findIndex(([stop]) => stop >= tone);
    if (upper === 0) return withAlpha(palette[0]![1], a);
    if (upper === -1) return withAlpha(palette.at(-1)![1], a);
    const [lowTone, low] = palette[upper - 1]!;
    const [highTone, high] = palette[upper]!;
    return withAlpha(mixOklab(low, high, (tone - lowTone) / (highTone - lowTone)), a);
}

function gamutColor(L: number, hue: number, chroma: number): Oklab {
    const at = (c: number) => ({ L, A: c * Math.cos(hue), B: c * Math.sin(hue) });
    if (inGamut(at(chroma))) return at(chroma);
    let [low, high] = [0, chroma];
    for (let step = 0; step < 16; step++) {
        const middle = (low + high) / 2;
        if (inGamut(at(middle))) low = middle;
        else high = middle;
    }
    return at(low);
}

/** A palette of [seed]'s hue at every tone, as close to its chroma as the sRGB gamut allows. */
export function paletteFromSeed(seed: Color): Palette {
    const lab = toOklab(seed);
    const hue = Math.atan2(lab.B, lab.A);
    const chroma = Math.hypot(lab.A, lab.B);
    return PALETTE_TONES.map(tone => {
        let [low, high] = [0, 1];
        for (let step = 0; step < 24; step++) {
            const middle = (low + high) / 2;
            if (toneOf(fromOklab(gamutColor(middle, hue, chroma), 1)) < tone) low = middle;
            else high = middle;
        }
        return [tone, fromOklab(gamutColor((low + high) / 2, hue, chroma), 1)];
    });
}
