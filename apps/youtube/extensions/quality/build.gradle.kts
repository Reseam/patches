// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

dependencies {
    compileOnly(project(":shared:bundle"))
    compileOnly(project(":shared:youtube-common"))
    compileOnly(project(":apps:youtube:extensions:controls"))
    compileOnly(project(":apps:youtube:extensions:video"))
    compileOnly(project(":apps:youtube:extensions:player"))
}
