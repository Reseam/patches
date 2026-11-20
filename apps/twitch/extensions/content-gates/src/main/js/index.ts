// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type GateProps = { contentGate?: string | null };

export function withoutMatureGate(props: GateProps | null | undefined) {
    return props?.contentGate === "mature" ? { ...props, contentGate: null } : props;
}
