// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { NativeModules } from "react-native";
import { refreshTheme } from "discord:modules/user_settings/ThemeActionCreators.tsx";
import { RESEAM_THEME_DOCUMENT, RESEAM_THEME_ID } from "./reseam";
import { parseTheme, serializeTheme, type Theme, type ThemeDocument, type ThemeEntry } from "./theme";
import { nativeColors, setRawColors, type ThemeBackground, themeBackground, themeVariant, variantName } from "./tokens";

// Kept in Discord's private storage, so Discord's synced appearance settings never change.

export type BackgroundStyle = "gradient" | "solid" | "none";

interface Library {
    themes: ThemeEntry[];
    applied: string | null;
    backgroundStyles: Record<string, BackgroundStyle>;
}

interface LibraryDocument {
    version: number;
    themes: { id: string; revision: number; theme: ThemeDocument }[];
    applied: string | null;
    backgroundStyles: Record<string, BackgroundStyle>;
}

const LIBRARY_VERSION = 1;

let library: Library | null = null;
let builtInThemes: ThemeEntry[] | null = null;
let preview: ThemeEntry | null = null;
let nativeVariant = "";
let snapshot = 0;
let variants: string[] | null = null;
let background: { entry: ThemeEntry | null; style: BackgroundStyle; value: ThemeBackground | null } = {
    entry: null,
    style: "gradient",
    value: null,
};
const listeners = new Set<() => void>();

export const withoutKey = <Value>(object: Record<string, Value>, key: string) =>
    Object.fromEntries(Object.entries(object).filter(([other]) => other !== key));

function loadLibrary(): Library {
    if (library !== null) return library;
    const saved = NativeModules.ReseamDiscordThemes.readThemes();
    if (saved === "") return (library = { themes: [], applied: null, backgroundStyles: {} });
    const document: LibraryDocument = JSON.parse(saved);
    return (library = {
        themes: document.themes.map(({ id, revision, theme }) => ({ id, revision, builtIn: false, theme: parseTheme(theme) })),
        applied: document.applied,
        backgroundStyles: document.backgroundStyles,
    });
}

function saveLibrary(saved: Library) {
    const document: LibraryDocument = {
        version: LIBRARY_VERSION,
        themes: saved.themes.map(entry => ({ id: entry.id, revision: entry.revision, theme: serializeTheme(entry.theme) })),
        applied: saved.applied,
        backgroundStyles: saved.backgroundStyles,
    };
    NativeModules.ReseamDiscordThemes.writeThemes(JSON.stringify(document));
}

function builtIns(): ThemeEntry[] {
    builtInThemes ??= [{ id: RESEAM_THEME_ID, revision: 1, builtIn: true, theme: parseTheme(RESEAM_THEME_DOCUMENT) }];
    return builtInThemes;
}

export const allThemes = () => [...builtIns(), ...loadLibrary().themes];

export const findTheme = (id: string) => allThemes().find(entry => entry.id === id) ?? null;

export function subscribe(listener: () => void) {
    listeners.add(listener);
    return () => void listeners.delete(listener);
}

/** Changes on every library change, for `useSyncExternalStore`. */
export const librarySnapshot = () => snapshot;

function notify() {
    snapshot++;
    variants = null;
    listeners.forEach(listener => listener());
}

export function appliedTheme(): ThemeEntry | null {
    const id = loadLibrary().applied;
    return id === null ? null : findTheme(id);
}

export function syncNativeColors() {
    const applied = appliedTheme();
    const variant = applied === null ? "" : variantName(applied);
    if (variant === nativeVariant) return;
    nativeVariant = variant;
    NativeModules.ReseamDiscordThemes.applyNativeColors(applied === null ? "" : JSON.stringify(nativeColors(applied)));
}

// A previewed theme shows in place of the applied one, except in native views.
export const shownTheme = () => preview ?? appliedTheme();

function shownThemeChanged() {
    setRawColors(shownTheme()?.theme ?? null);
    refreshTheme();
    notify();
}

function appliedThemeChanged() {
    syncNativeColors();
    shownThemeChanged();
}

export const backgroundStyle = (id: string): BackgroundStyle => loadLibrary().backgroundStyles[id] ?? "gradient";

export function setBackgroundStyle(id: string, style: BackgroundStyle) {
    const saved = loadLibrary();
    const others = withoutKey(saved.backgroundStyles, id);
    saved.backgroundStyles = style === "gradient" ? others : { ...others, [id]: style };
    saveLibrary(saved);
    notify();
}

export function shownBackground(): ThemeBackground | null {
    const shown = shownTheme();
    const style = shown === null ? "gradient" : backgroundStyle(shown.id);
    if (background.entry !== shown || background.style !== style) {
        const value = shown === null || style === "none" ? null : themeBackground(shown, style === "gradient");
        background = { entry: shown, style, value };
    }
    return background.value;
}

export function applyTheme(id: string | null) {
    const saved = loadLibrary();
    if (saved.applied === id) return;
    saved.applied = id;
    saveLibrary(saved);
    appliedThemeChanged();
}

const newThemeId = () => `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;

export function addTheme(theme: Theme): ThemeEntry {
    const entry = { id: newThemeId(), revision: 1, builtIn: false, theme };
    const saved = loadLibrary();
    saved.themes.push(entry);
    saveLibrary(saved);
    notify();
    return entry;
}

export function findSameTheme(theme: Theme): ThemeEntry | null {
    const text = JSON.stringify(serializeTheme(theme));
    return allThemes().find(entry => JSON.stringify(serializeTheme(entry.theme)) === text) ?? null;
}

export function updateTheme(id: string, theme: Theme): ThemeEntry {
    const saved = loadLibrary();
    const previous = saved.themes.find(entry => entry.id === id)!;
    const entry = { ...previous, revision: previous.revision + 1, theme };
    saved.themes = saved.themes.map(other => (other === previous ? entry : other));
    saveLibrary(saved);
    if (saved.applied === id) appliedThemeChanged();
    else notify();
    return entry;
}

export function deleteTheme(id: string) {
    const saved = loadLibrary();
    saved.themes = saved.themes.filter(entry => entry.id !== id);
    saved.backgroundStyles = withoutKey(saved.backgroundStyles, id);
    const wasApplied = saved.applied === id;
    if (wasApplied) saved.applied = null;
    saveLibrary(saved);
    if (wasApplied) appliedThemeChanged();
    else notify();
}

export function setPreview(entry: ThemeEntry | null) {
    if (preview === entry) return;
    preview = entry;
    shownThemeChanged();
}

/** The variants of the shown and applied themes, compiled into Discord's tables. */
export function themeVariants(): string[] {
    variants ??= [shownTheme(), appliedTheme()].filter(entry => entry !== null).map(themeVariant);
    return variants;
}

export const themeExperimentsKey = () => themeVariants().join(",");
