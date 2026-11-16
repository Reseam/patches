// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { type ReactElement, cloneElement, createElement, isValidElement } from "react";
import { IconRefresh } from "twitch:icons/IconRefresh";

const SETTINGS_BUTTON = "player-controls-settings";
const REFRESH_BUTTON = "player-controls-reseam-refresh";
const NOT_REFRESHING = Symbol("not refreshing");

type TheatrePlayer = { playerElement?: unknown; handlePlayerErrorReset?: () => void };

// The theatre gives its Retry only to the error overlay, so the button keeps the one its player hook returned last.
let retry: (() => void) | undefined;
let accessToken: unknown;
// Holds the token that Retry replaces, until the new token arrives.
let staleToken: unknown = NOT_REFRESHING;
let restarts = 0;

// The theatre reuses its player element between renders, so this keeps one restarted copy per element.
const restartedCopies = new WeakMap<object, { restarts: number; element: ReactElement }>();

function restarted(element: ReactElement) {
    const copy = restartedCopies.get(element);
    if (copy?.restarts === restarts) return copy.element;
    const next = cloneElement(element, { key: `${element.key ?? ""}:reseam-restart-${restarts}` });
    restartedCopies.set(element, { restarts, element: next });
    return next;
}

// Retry loads a new source into the running player, which keeps showing the old picture and can stay frozen.
// Once Retry's new access token arrives, a new key makes React mount a new player for it.
export function restartablePlayer(
    player: TheatrePlayer | null | undefined,
    props: { playerAccessToken?: unknown } | null | undefined,
) {
    retry = player?.handlePlayerErrorReset;
    const token = props?.playerAccessToken;
    if (staleToken !== NOT_REFRESHING && token !== staleToken) {
        staleToken = NOT_REFRESHING;
        restarts++;
    }
    accessToken = token;
    const element = player?.playerElement;
    return restarts === 0 || !isValidElement(element) ? player : { ...player, playerElement: restarted(element) };
}

const refresh = () => {
    if (retry === undefined) return;
    staleToken = accessToken;
    retry();
};

const testId = (node: unknown) => (isValidElement<any>(node) ? node.props.testID : undefined);

function refreshButton(gear: any, label: string) {
    const icon = gear.props.icon;
    return cloneElement(gear, {
        key: REFRESH_BUTTON,
        testID: REFRESH_BUTTON,
        accessibilityLabel: label,
        disabled: false,
        onPress: refresh,
        icon: isValidElement<any>(icon) ? createElement(IconRefresh, icon.props) : icon,
    });
}

// Puts the button just before the settings gear, wherever the overlay nests it.
function withButton(node: any, label: string): any {
    if (Array.isArray(node)) {
        let changed = false;
        const next = node.map(child => {
            const updated = withButton(child, label);
            changed ||= updated !== child;
            return updated;
        });
        return changed ? next : node;
    }
    if (!isValidElement<any>(node)) return node;
    const children = node.props.children;
    if (Array.isArray(children)) {
        const gearIndex = children.findIndex(child => testId(child) === SETTINGS_BUTTON);
        if (gearIndex >= 0) {
            if (children.some(child => testId(child) === REFRESH_BUTTON)) return node;
            const next = [...children];
            next.splice(gearIndex, 0, refreshButton(children[gearIndex], label));
            return cloneElement(node, { children: next });
        }
    }
    if (children == null) return node;
    const next = withButton(children, label);
    return next === children ? node : cloneElement(node, { children: next });
}

// The overlay reuses its element between renders, so this keeps one copy with the button per element.
const copiesWithButton = new WeakMap<object, unknown>();

export function withRefreshButton(element: unknown, label: string) {
    if (!isValidElement(element)) return element;
    let next = copiesWithButton.get(element);
    if (next === undefined) {
        next = withButton(element, label);
        copiesWithButton.set(element, next);
    }
    return next;
}
