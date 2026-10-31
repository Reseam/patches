// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { useMemo, useSyncExternalStore } from "react";
import {
    appliedTheme,
    shownBackground,
    shownTheme,
    subscribe,
    syncNativeColors,
    themeExperimentsKey,
    themeVariants,
} from "./store";
import type { ThemeBase } from "./theme";
import { captureExperiments, captureTokens, setRawColors } from "./tokens";

/** Discord's color tokens loaded: the applied theme's raw colors go in before any code reads them. */
export function tokensLoaded(tokens: Parameters<typeof captureTokens>[0]) {
    captureTokens(tokens);
    const applied = appliedTheme();
    if (applied !== null) setRawColors(applied.theme);
    syncNativeColors();
}

export function experimentsLoaded(definitions: Parameters<typeof captureExperiments>[0]) {
    captureExperiments(definitions);
}

// Every theme context inherits the root's experiments, and every style cache keys on them.
export function useThemeContextProps<Props extends { enabledExperiments?: string[] }>(props: Props): Props {
    const key = useSyncExternalStore(subscribe, themeExperimentsKey);
    const discordExperiments = props.enabledExperiments;
    const enabledExperiments = useMemo(
        () => [...(key === "" ? [] : key.split(",")), ...(discordExperiments ?? [])],
        [key, discordExperiments],
    );
    return { ...props, enabledExperiments };
}

export function withThemeVariants<Context extends { enabledExperiments?: string[] }>(context: Context): Context {
    const themes = themeVariants();
    if (themes.length === 0 || context.enabledExperiments?.[0] === themes[0]) return context;
    return { ...context, enabledExperiments: [...themes, ...(context.enabledExperiments ?? [])] };
}

// Native style properties cache by their arguments, in a context of their own.
export function keyedByTheme<Args extends unknown[], Result>(resolve: (...args: [...Args, string]) => Result) {
    return (...args: Args) => resolve(...args, themeExperimentsKey());
}

export const shownBase = (): ThemeBase | null => shownTheme()?.theme.base ?? null;

export const isThemeShown = () => shownTheme() !== null;

export { shownBackground };

export function useThemeBackground<Background>(background: Background) {
    const entry = useSyncExternalStore(subscribe, shownTheme);
    const themeBackground = useSyncExternalStore(subscribe, shownBackground);
    return entry === null ? background : themeBackground;
}

export function useThemeType<Type>(type: Type) {
    const entry = useSyncExternalStore(subscribe, shownTheme);
    const background = useSyncExternalStore(subscribe, shownBackground);
    if (entry === null) return type;
    return background === null ? "default" : "custom";
}

export function useDisplaySettings<Settings>(settings: Settings, saved: unknown) {
    const entry = useSyncExternalStore(subscribe, shownTheme);
    const background = useSyncExternalStore(subscribe, shownBackground);
    if (entry === null || saved != null) return settings;
    return background === null ? undefined : { baseTheme: entry.theme.base, customTheme: background.customThemeSettings };
}

export function useGuildTheme<GuildTheme>(guildTheme: GuildTheme) {
    const entry = useSyncExternalStore(subscribe, shownTheme);
    return entry === null ? guildTheme : null;
}
