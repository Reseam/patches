extra["dexOutputName"] = "instagram-download.dex"

apply(from = rootDir.resolve("android-extension-module.gradle.kts"))

dependencies {
    add("compileOnly", project(":shared-settings-runtime"))
    add("compileOnly", project(":apps:instagram:extensions:refs"))
}
