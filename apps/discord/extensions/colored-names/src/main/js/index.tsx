// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { cloneElement, isValidElement, type ReactElement } from "react";
import { Text } from "react-native";
import ChannelMemberStore, { EVERYONE_CHANNEL_ID } from "discord:stores/ChannelMemberStore.tsx";
import GuildMemberStore from "discord:stores/GuildMemberStore.tsx";
import GuildRoleStore from "discord:stores/GuildRoleStore.tsx";
import UserStore from "discord:stores/UserStore.tsx";

const memberColor = (guildId: string, userId: string): string | undefined =>
    GuildMemberStore.getMember(guildId, userId)?.colorString ?? undefined;

const withColor = (element: ReactElement<any>, color: string) =>
    cloneElement(element, { style: [element.props.style, { color }] });

// The same fields that carry a role mention's color.
export function colorMention(mention: any) {
    const colorString = mention.guildId && mention.userId && memberColor(mention.guildId, mention.userId);
    if (!colorString) return mention;
    const color = parseInt(colorString.slice(1), 16);
    return { ...mention, roleColor: color, color, colorString };
}

// "Name is typing...": Discord renders one element per typing user it knows, in the order of
// `typingUserIds`; four or more typing users become one plain sentence.
export function colorTypingText(text: unknown, props: { guildId?: string; typingUserIds: string[] }) {
    const { guildId } = props;
    if (!Array.isArray(text) || guildId == null) return text;
    const users = props.typingUserIds.filter(id => UserStore.getUser(id) != null);
    let user = 0;
    return text.map(part => {
        if (!isValidElement<any>(part)) return part;
        const color = memberColor(guildId, users[user++]!);
        return color ? withColor(part, color) : part;
    });
}

// A voice participant row: its first child is the name, followed by an optional server tag.
export function colorVoiceName(row: ReactElement<any>, props: { member?: { colorString?: string } }) {
    const color = props.member?.colorString;
    const [name, ...rest] = row.props.children;
    return color && name ? cloneElement(row, {}, withColor(name, color), ...rest) : row;
}

// Member list headers: section `index` shows group `index` as "title — count"; a hoisted role's
// group takes the role's color. The search results header shares index 0, hence the title check.
export function colorMemberListHeaders(list: ReactElement<any> | null, props: { guildId: string; channelId: string }) {
    const children = list?.props.children;
    if (list == null || !Array.isArray(children)) return list;
    const { guildId } = props;
    const channelId = props.channelId === EVERYONE_CHANNEL_ID ? null : props.channelId;
    const colored = (getSectionProps: (index: number) => any) => (index: number) => {
        const section = getSectionProps(index);
        const group = ChannelMemberStore.getProps(guildId, channelId).groups[index];
        const color: string | undefined = group && GuildRoleStore.getRole(guildId, group.id)?.colorString;
        if (section?.type !== "section" || !color || section.props.title !== `${group.title} — ${group.count}`) {
            return section;
        }
        return { ...section, props: { ...section.props, title: <Text style={{ color }}>{section.props.title}</Text> } };
    };
    return cloneElement(
        list,
        {},
        ...children.map(child =>
            child?.props?.getSectionProps == null
                ? child
                : cloneElement(child, { getSectionProps: colored(child.props.getSectionProps) }),
        ),
    );
}
