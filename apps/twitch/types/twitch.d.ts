// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Twitch's modules that twitchModules declares, such as `twitch:icons/IconRefresh`.
declare module "twitch:*";

// React Native's native modules, which each Kotlin extension's generated declarations extend.
declare module "react-native" {
    export interface NativeModulesStatic {}
    export const NativeModules: NativeModulesStatic;
}

// React Native's timer, missing from the ES2023 library.
declare function setTimeout(callback: () => void, delayMillis: number): number;
