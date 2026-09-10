# Reseam patches

The official patch bundle for Reseam. Each app under `apps/` has a `patch` module with the patches and, where needed, extension modules with code that gets injected into the app. The `app.reseam.workspace` Gradle plugin configures every module from the directory layout.

## Setup

- JDK 17.
- Android SDK, with `ANDROID_HOME` pointing at it.
- The `reseam` CLI on `PATH`, or its path in `RESEAM_BIN`. Get the release that matches the plugin version in `settings.gradle.kts`.
- A signing key: `reseam bundle keygen --out ~/.reseam/bundle-signing.key`.

## Build

```shell
./gradlew bundle
```

Writes `build/reseam/reseam-patches.reseam`. Try it on an APK:

```shell
reseam patch app.apk --bundle build/reseam/reseam-patches.reseam --trust <your public key> --output patched.apk
```

To build against a local engine checkout instead of the published SDK and plugin, set `RESEAM_WORKSPACE=/path/to/reseam`.

How patches are written is documented in the engine repository under `docs/`.

## Release

Tag `vX.Y.Z`. CI builds and signs the bundle, writes `patches.json`, and uploads both to the Forgejo release.
