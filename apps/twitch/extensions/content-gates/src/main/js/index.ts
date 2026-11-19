// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type GateProps = { contentGate?: string | null };

// This removes only the mature gate. Parental controls, your own content limits and the age check have their own gates.
export function withoutMatureGate(props: GateProps | null | undefined) {
    return props?.contentGate === "mature" ? { ...props, contentGate: null } : props;
}
