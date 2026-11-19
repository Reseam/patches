// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Extensions import Twitch's JavaScript modules by the names that twitchModules declares, such as `twitch:icons/IconRefresh`.
declare module "twitch:*";

// React Native has this timer, but the ES2023 library that this check uses does not declare it.
declare function setTimeout(callback: () => void, delayMillis: number): number;
