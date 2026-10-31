// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { cloneElement, type ReactElement } from "react";
import MessageActions from "discord:actions/MessageActionCreators.tsx";

const BUTTON_GAP = 8;

// Voice channel chats show a dismiss button in place of Jump to Present.
export function withFirstMessageButton(
    container: ReactElement<any> | null,
    props: { channelId: string; onJumpToPresent: unknown },
) {
    const jumpToPresent = container?.props.children;
    if (container == null || jumpToPresent?.props.onPress !== props.onJumpToPresent) return container;
    const Container = container.type;
    return cloneElement(
        container,
        {},
        <Container key="first" style={{ marginBottom: BUTTON_GAP, transform: [{ rotate: "180deg" }] }}>
            {cloneElement(jumpToPresent, {
                accessibilityLabel: "Jump to first message",
                // A channel's ID is older than every message in it, so jumping to it loads the first
                // messages.
                onPress: () =>
                    MessageActions.jumpToMessage({ channelId: props.channelId, messageId: props.channelId, flash: false }),
            })}
        </Container>,
        cloneElement(jumpToPresent, { key: "present" }),
    );
}
