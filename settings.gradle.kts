// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

rootProject.name = "reseam-patches"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://git.reseam.app/api/packages/reseam/maven") {
            mavenContent { includeGroup("app.reseam") }
        }
    }
}

val reseamWorkspace: String? = (settings.providers.gradleProperty("reseam.workspace").orNull
    ?: System.getenv("RESEAM_WORKSPACE"))?.takeIf { it.isNotBlank() }

if (reseamWorkspace != null) {
    val workspaceDir = file(reseamWorkspace)
    require(workspaceDir.isDirectory) {
        "reseam.workspace points to a missing directory: $workspaceDir"
    }
    includeBuild(workspaceDir)
}

include(":shared-settings-runtime")
project(":shared-settings-runtime").projectDir = file("shared/settings-runtime")

include(":apps:instagram:patch")
project(":apps:instagram:patch").projectDir = file("apps/instagram/patch")

include(":apps:instagram:extensions:settings-entry")
project(":apps:instagram:extensions:settings-entry").projectDir =
    file("apps/instagram/extensions/settings-entry")

include(":apps:instagram:extensions:download")
project(":apps:instagram:extensions:download").projectDir =
    file("apps/instagram/extensions/download")

include(":apps:instagram:extensions:follows")
project(":apps:instagram:extensions:follows").projectDir =
    file("apps/instagram/extensions/follows")

include(":apps:instagram:extensions:refs")
project(":apps:instagram:extensions:refs").projectDir =
    file("apps/instagram/extensions/refs")

include(":apps:telegram:patch")
project(":apps:telegram:patch").projectDir = file("apps/telegram/patch")

include(":apps:telegram:extensions:settings-entry")
project(":apps:telegram:extensions:settings-entry").projectDir =
    file("apps/telegram/extensions/settings-entry")

include(":apps:telegram:extensions:anti-delete")
project(":apps:telegram:extensions:anti-delete").projectDir =
    file("apps/telegram/extensions/anti-delete")

