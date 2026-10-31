// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { Permissions } from "discord:../discord_common/js/shared/Constants.tsx";
import { combine, hasAny } from "discord:../discord_common/js/shared/utils/BigFlagUtils.tsx";
import GuildStore from "discord:stores/GuildStore.tsx";
import { computePermissions } from "discord:utils/PermissionUtils.tsx";

interface StaffTag {
    text: string;
    color: number;
    qualifies(guild: any, user: any, permissions: unknown): boolean;
}

const holdsAny =
    (...permissions: unknown[]): StaffTag["qualifies"] =>
    (_guild, _user, held) =>
        hasAny(held, combine(...permissions));

// Ordered by rank; an author shows the first tag they qualify for.
const TAGS: StaffTag[] = [
    { text: "OWNER", color: 0xf0b232, qualifies: (guild, user) => guild.ownerId === user.id },
    { text: "ADMIN", color: 0xe5484d, qualifies: holdsAny(Permissions.ADMINISTRATOR) },
    {
        text: "MANAGER",
        color: 0x2fa36b,
        qualifies: holdsAny(Permissions.MANAGE_GUILD, Permissions.MANAGE_ROLES, Permissions.MANAGE_CHANNELS),
    },
    {
        text: "MOD",
        color: 0x5865f2,
        qualifies: holdsAny(
            Permissions.MANAGE_MESSAGES,
            Permissions.KICK_MEMBERS,
            Permissions.BAN_MEMBERS,
            Permissions.MODERATE_MEMBERS,
        ),
    },
];

// Android reads colors as signed 32-bit ARGB.
const opaque = (rgb: number) => (0xff000000 | rgb) | 0;

function staffTag(channel: any, user: any): StaffTag | undefined {
    const guild = channel?.guild_id && GuildStore.getGuild(channel.guild_id);
    if (!guild) return undefined;
    const permissions = computePermissions({ user, context: guild, checkElevated: false });
    return TAGS.find(tag => tag.qualifies(guild, user, permissions));
}

export function withStaffTag(tag: any, properties: { message: { author: any }; channel: unknown }) {
    const { author } = properties.message;
    const staff = tag.tagText == null && !author.bot ? staffTag(properties.channel, author) : undefined;
    return staff === undefined
        ? tag
        : {
              ...tag,
              tagText: staff.text,
              tagAccessibilityLabel: staff.text,
              tagTextColor: opaque(0xffffff),
              tagBackgroundColor: opaque(staff.color),
          };
}
