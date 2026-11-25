// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { type ReactNode, cloneElement, isValidElement } from "react";
import { NativeModules, type StyleProp, StyleSheet, type ViewStyle } from "react-native";

type Emote = { code: string; id: string; url: string; aspectRatio: number };
type EmotePosition = { id: string; start: number; end: number };
type ChatMessage = { body?: string | null; emotes?: readonly EmotePosition[] | null; sourceRoomID?: string | null };
type RowProps = { message?: ChatMessage | null; channelID?: string | null };
type EmotePartProps = { emoteId?: string | null };
type PartNodeProps = { onPress?: unknown; src?: string; style?: StyleProp<ViewStyle>; children?: ReactNode };

const emotesByChannel = new Map<string, Map<string, Emote>>();
const emotesById = new Map<string, Emote>();
const messageCopies = new WeakMap<ChatMessage, ChatMessage>();

// Later emotes override earlier ones with the same code.
function emotesOf(channelId: string) {
    let emotes = emotesByChannel.get(channelId);
    if (emotes === undefined) {
        const list = NativeModules.ReseamTwitchEmotes.emotes(channelId);
        if (list == null) return undefined;
        emotes = new Map();
        for (const emote of JSON.parse(list) as Emote[]) {
            emotes.set(emote.code, emote);
            emotesById.set(emote.id, emote);
        }
        emotesByChannel.set(channelId, emotes);
    }
    return emotes;
}

// Twitch counts emote positions in code points, with an inclusive end.
function withEmotes(message: ChatMessage, body: string, emotes: Map<string, Emote>): ChatMessage {
    const twitchEmotes = message.emotes ?? [];
    const added: EmotePosition[] = [];
    const characters = Array.from(body);
    let start = 0;
    for (let index = 0; index <= characters.length; index++) {
        const character = characters[index];
        if (character !== undefined && !/\s/.test(character)) continue;
        const end = index - 1;
        const emote = end >= start ? emotes.get(characters.slice(start, index).join("")) : undefined;
        if (emote !== undefined && !twitchEmotes.some(it => it.start <= end && it.end >= start)) added.push({ id: emote.id, start, end });
        start = index + 1;
    }
    return added.length === 0 ? message : { ...message, emotes: [...twitchEmotes, ...added] };
}

/** Marks the 7TV, BetterTTV and FrankerFaceZ emotes in a chat row's message, once the channel's emotes are known. */
export function withThirdPartyEmotes(props: RowProps | null | undefined) {
    const message = props?.message;
    const body = message?.body;
    const channelId = message?.sourceRoomID || props?.channelID;
    if (props == null || message == null || !body || !channelId) return props;
    let copy = messageCopies.get(message);
    if (copy === undefined) {
        const emotes = emotesOf(channelId);
        if (emotes === undefined) return props;
        copy = withEmotes(message, body, emotes);
        messageCopies.set(message, copy);
    }
    return copy === message ? props : { ...props, message: copy };
}

// Twitch's emote card knows only Twitch emotes, so a third-party emote loses the press that opens it.
function drawn(node: ReactNode, emote: Emote): ReactNode {
    if (!isValidElement<PartNodeProps>(node)) return node;
    const { onPress, src, style, children } = node.props;
    if (onPress) return drawn(children, emote);
    const height = StyleSheet.flatten(style)?.height;
    const sized = typeof height === "number" ? [style, { width: height * emote.aspectRatio }] : style;
    return src === undefined ? cloneElement(node, { style: sized }, drawn(children, emote)) : cloneElement(node, { src: emote.url, style: sized });
}

/** Draws [part], the emote part Twitch drew for [props], with the third-party emote that [props] name. */
export function thirdPartyEmotePart(props: EmotePartProps | null | undefined, part: ReactNode) {
    const emote = props?.emoteId == null ? undefined : emotesById.get(props.emoteId);
    return emote === undefined ? part : drawn(part, emote);
}
