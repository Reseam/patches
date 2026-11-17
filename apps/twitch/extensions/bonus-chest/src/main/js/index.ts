// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type ComposerProps = { hasClaimable?: boolean; bonusUUID?: string | null; onClaimBonus?: (() => unknown) | null };

// The chat bar renders many times while one chest is up, so each bonus is claimed once.
const claimedBonuses = new Set<string>();

// React must not update state while it renders the chat bar, so the claim runs once rendering returns.
export function claimBonusChest(props: ComposerProps | null | undefined) {
    const bonus = props?.bonusUUID;
    const claim = props?.onClaimBonus;
    if (!props?.hasClaimable || !bonus || typeof claim !== "function" || claimedBonuses.has(bonus)) return;
    claimedBonuses.add(bonus);
    Promise.resolve().then(() => claim());
}
