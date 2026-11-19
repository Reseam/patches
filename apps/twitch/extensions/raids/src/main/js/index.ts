// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type CarryProps = { going?: { isCreator?: boolean } | null };

// A raid you start still takes you to the channel you raid.
export function withoutCarry(props: CarryProps | null | undefined) {
    return props?.going && !props.going.isCreator ? { ...props, going: null } : props;
}
