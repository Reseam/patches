// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.dislike.requests;

import static app.reseam.youtube.dislike.requests.Route.Method.GET;
import static app.reseam.youtube.dislike.requests.Route.Method.POST;

import java.io.IOException;
import java.net.HttpURLConnection;

import app.reseam.youtube.dislike.requests.Requester;
import app.reseam.youtube.dislike.requests.Route;

class ReturnYouTubeDislikeRoutes {
    static final String RYD_API_URL = "https://returnyoutubedislikeapi.com/";

    static final Route SEND_VOTE = new Route(POST, "interact/vote");
    static final Route CONFIRM_VOTE = new Route(POST, "interact/confirmVote");
    static final Route GET_DISLIKES = new Route(GET, "votes?videoId={video_id}");
    static final Route GET_REGISTRATION = new Route(GET, "puzzle/registration?userId={user_id}");
    static final Route CONFIRM_REGISTRATION = new Route(POST, "puzzle/registration?userId={user_id}");

    private ReturnYouTubeDislikeRoutes() {
    }

    static HttpURLConnection getRYDConnectionFromRoute(Route route, String... params) throws IOException {
        return Requester.getConnectionFromRoute(RYD_API_URL, route, params);
    }

}
