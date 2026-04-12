extra["dexOutputName"] = "instagram-settings.dex"
extra["dexExcludeClasses"] = "com/instagram/base/activity/IgActivity.class"

apply(from = rootDir.resolve("android-extension-module.gradle.kts"))

dependencies {
    add("compileOnly", project(":shared-settings-runtime"))
}
