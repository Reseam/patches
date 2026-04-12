rootProject.name = "stitch-patches"

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
