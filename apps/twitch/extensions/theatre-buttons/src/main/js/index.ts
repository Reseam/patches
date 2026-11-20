// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { cloneElement, isValidElement } from "react";

export function withoutClipAction(props: { onClips?: unknown } | null | undefined) {
    return props?.onClips === undefined ? props : { ...props, onClips: undefined };
}

const testId = (node: unknown) => (isValidElement<any>(node) ? node.props.testID : undefined);

// Leaves null in the button's place, so the buttons after it keep their positions.
function without(node: any, button: string): any {
    if (testId(node) === button) return null;
    if (Array.isArray(node)) {
        let changed = false;
        const next = node.map(child => {
            const updated = without(child, button);
            changed ||= updated !== child;
            return updated;
        });
        return changed ? next : node;
    }
    if (!isValidElement<any>(node) || node.props.children == null) return node;
    const children = without(node.props.children, button);
    return children === node.props.children ? node : cloneElement(node, { children });
}

// The overlay reuses its element between renders.
const copiesWithout = new Map<string, WeakMap<object, unknown>>();

export function withoutButton(element: unknown, button: string) {
    if (!isValidElement(element)) return element;
    let copies = copiesWithout.get(button);
    if (copies === undefined) {
        copies = new WeakMap();
        copiesWithout.set(button, copies);
    }
    let next = copies.get(element);
    if (next === undefined) {
        next = without(element, button);
        copies.set(element, next);
    }
    return next;
}
