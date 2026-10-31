// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { ButtonStyle, ComponentType, UnfurledMediaLoadingState } from "discord:flow/Server.tsx";
import { transformComponents } from "discord:modules/interaction_components/InteractionComponentUtils.tsx";

const ADD_LINK = /https:\/\/reseam\.app\/add\/?\?[^\s<>]+/g;
const SOURCE = /^https?:\/\/([^/?#]+)([^?#]*)/;
const GITHUB_REPOSITORY = /^\/([\w.-]+)\/([\w.-]+?)(?:\.git)?\/?$/;
const MAX_CARDS = 5;
const RESEAM_MINT = 0xa2ebbd;
const AVATAR_SIZE = 96;

const isAddLink = (url: string | undefined) => url?.match(ADD_LINK)?.[0] === url;

const escapeMarkdown = (text: string) => text.replace(/[\\*_~`|#>[\]()-]/g, "\\$&");

function source(link: string) {
    const url = decodeURIComponent(link.match(/[?&]url=([^&#]+)/)?.[1] ?? "");
    const [, host = url, path = ""] = url.match(SOURCE) ?? [];
    const repository = host === "github.com" ? path.match(GITHUB_REPOSITORY) : null;
    return {
        name: repository ? `${repository[1]}/${repository[2]}` : `${host}${path.replace(/\/$/, "")}`,
        host,
        avatar: repository && `https://avatars.githubusercontent.com/${repository[1]}?s=${AVATAR_SIZE}`,
    };
}

function card(link: string) {
    const { name, host, avatar } = source(link);
    const text = {
        type: ComponentType.TEXT_DISPLAY,
        content: `-# Reseam patch source\n**${escapeMarkdown(name)}**\n-# ${escapeMarkdown(host)}`,
    };
    const thumbnail = avatar && {
        type: ComponentType.THUMBNAIL,
        spoiler: false,
        media: {
            url: avatar,
            proxy_url: avatar,
            width: AVATAR_SIZE,
            height: AVATAR_SIZE,
            content_type: "image/png",
            loading_state: UnfurledMediaLoadingState.LOADED_SUCCESS,
        },
    };
    return {
        type: ComponentType.CONTAINER,
        accent_color: RESEAM_MINT,
        spoiler: false,
        components: [
            thumbnail ? { type: ComponentType.SECTION, components: [text], accessory: thumbnail } : text,
            {
                type: ComponentType.ACTION_ROW,
                components: [
                    { type: ComponentType.BUTTON, style: ButtonStyle.LINK, label: "Add to Reseam Manager", url: link },
                ],
            },
        ],
    };
}

// Discord numbers components by position, so a message with its own components keeps them alone.
export function withAddCards(args: any) {
    const { message } = args;
    const links = [...new Set<string>(message.content.match(ADD_LINK))].slice(0, MAX_CARDS);
    if (links.length === 0 || message.components.length > 0) return args;
    return {
        ...args,
        message: message
            .set("components", transformComponents(links.map(card)))
            .set("embeds", message.embeds.filter((embed: { url?: string }) => !isAddLink(embed.url))),
    };
}
