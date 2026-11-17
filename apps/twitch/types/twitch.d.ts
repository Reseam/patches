// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Twitch's own source files, imported by name, such as `twitch:icons/IconRefresh`.
declare module "twitch:*";

// React Native's timer, which the ES2023 library this check uses does not declare.
declare function setTimeout(callback: () => void, delayMillis: number): number;
