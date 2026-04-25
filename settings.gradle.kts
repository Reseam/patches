// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

rootProject.name = "reseam-patches"

includeBuild("../reseam/kotlin-sdk")

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
