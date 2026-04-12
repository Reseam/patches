extra["dexOutputName"] = "instagram-follows-you.dex"

apply(from = rootDir.resolve("android-extension-module.gradle.kts"))

dependencies {
    add("compileOnly", project(":shared-settings-runtime"))
}
