// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { cloneElement, type ReactElement } from "react";
import { ActionSheetRow, TableRow } from "discord:design/native.tsx";
import GuildChannelStore from "discord:stores/GuildChannelStore.tsx";
import GuildMemberCountStore from "discord:stores/GuildMemberCountStore.tsx";
import GuildMemberStore from "discord:stores/GuildMemberStore.tsx";
import GuildRoleStore from "discord:stores/GuildRoleStore.tsx";
import UserStore from "discord:stores/UserStore.tsx";
import SnowflakeUtils from "discord:utils/SnowflakeUtils.tsx";

const VERIFICATION_LEVELS = ["None", "Low", "Medium", "High", "Highest"];

const formatDate = (date: Date) =>
    date.toLocaleDateString(undefined, { year: "numeric", month: "long", day: "numeric" });

function details(guild: any): [string, string][] {
    const owner = UserStore.getUser(guild.ownerId);
    const joinedAt = GuildMemberStore.getMember(guild.id, UserStore.getCurrentUser().id)?.joinedAt;
    const memberCount: number | undefined = GuildMemberCountStore.getMemberCount(guild.id);
    const channels =
        GuildChannelStore.getSelectableChannelIds(guild.id).length + GuildChannelStore.getVocalChannelIds(guild.id).length;
    const rows: [string, string | undefined][] = [
        ["Owner", owner ? `@${owner.username}` : guild.ownerId],
        ["Created", formatDate(new Date(SnowflakeUtils.extractTimestamp(guild.id)))],
        ["Joined", joinedAt && formatDate(new Date(joinedAt))],
        ["Members", memberCount?.toLocaleString()],
        ["Roles", String(GuildRoleStore.getNumRoles(guild.id))],
        ["Channels", String(channels)],
        ["Boosts", `${guild.premiumSubscriberCount ?? 0} (level ${guild.premiumTier ?? 0})`],
        ["Verification", VERIFICATION_LEVELS[guild.verificationLevel]],
        ["Server ID", guild.id],
    ];
    return rows.filter((row): row is [string, string] => row[1] !== undefined);
}

export function withDetails(actions: ReactElement | null, props: { guild: any }) {
    const info = (
        <ActionSheetRow.Group key="server-details">
            {details(props.guild).map(([label, value]) => (
                <ActionSheetRow key={label} label={label} trailing={<TableRow.TrailingText text={value} />} />
            ))}
        </ActionSheetRow.Group>
    );
    return actions == null ? info : [cloneElement(actions, { key: "actions" }), info];
}
