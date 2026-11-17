// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// The Java side of Auto-claim rewards may claim the same bonus first, which clears the chest, so the chat bar waits this long before it claims.
const CLAIM_DELAY_MILLIS = 2000;

type ComposerProps = {
    hasClaimable?: boolean;
    hasClaimError?: boolean;
    bonusUUID?: string | null;
    onClaimBonus?: (() => unknown) | null;
};

let lastBonusId: string | undefined;
let latestProps: ComposerProps | undefined;

function claimable(props: ComposerProps | undefined, bonusId: string) {
    return props?.hasClaimable === true && props.hasClaimError !== true && props.bonusUUID === bonusId && typeof props.onClaimBonus === "function";
}

// Tapping the chest calls onClaimBonus, so this calls it for each new bonus that is still up and has no error when the delay ends.
export function claimBonusChest(props: ComposerProps | null | undefined) {
    if (props == null) return;
    latestProps = props;
    const bonusId = props.bonusUUID;
    if (!bonusId || bonusId === lastBonusId || !claimable(props, bonusId)) return;
    lastBonusId = bonusId;
    setTimeout(() => {
        if (claimable(latestProps, bonusId)) latestProps?.onClaimBonus?.();
    }, CLAIM_DELAY_MILLIS);
}
