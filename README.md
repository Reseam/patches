<p align="center">
  <img src="https://reseam.app/logo.svg" alt="Reseam logo" width="96">
</p>

<h1 align="center">Reseam Patches</h1>

<p align="center">
  <a href="https://reseam.app/patches/">Browse the patches</a> ·
  <a href="https://reseam.app/download/">Get Reseam Manager</a>
</p>

The official patch bundle for Reseam. Reseam Manager includes it by default, and [reseam.app/patches](https://reseam.app/patches/) lists every patch in it.

To write your own bundle, start from the [patch bundle template](https://git.reseam.app/reseam/patches-template) and the [docs](https://reseam.app/docs/authoring/start/). This README covers building this repository.

## Layout

```text
apps/<app>/patch        the patches for one app
apps/<app>/extensions   code added to that app, when its patches need it
apps/universal          patches that work on any app
shared/                 code used by several apps, such as the in-app settings screen
manifest.toml           bundle name, author, and description
```

The `app.reseam.workspace` Gradle plugin configures every module from this layout. Its version is pinned in `settings.gradle.kts`.

## Build

You need:

- JDK 17, and the Android SDK with `ANDROID_HOME` set.
- The `reseam` CLI at the same version as the plugin, on `PATH` or in `RESEAM_BIN`.
- A signing key: `reseam bundle keygen --out ~/.reseam/bundle-signing.key`.

```shell
./gradlew bundle
```

This writes `build/reseam/reseam-patches.reseam`. Try it on an APK, trusting your own key:

```shell
reseam patch app.apk --bundle build/reseam/reseam-patches.reseam --trust <your public key>
```

When changing the engine at the same time, set `RESEAM_WORKSPACE=/path/to/reseam` to build against that checkout instead of the published engine.

## Release

Push a `vX.Y.Z` tag. CI builds and signs the bundle with the official key, writes `patches.json`, and uploads both. Reseam Manager picks up the new release on its next bundle check.

## License

AGPL-3.0-or-later, with additional terms under section 7 in [NOTICE](NOTICE). Code ported from ReVanced stays GPL-3.0-or-later without those terms, and a few files carry other licenses; see `REUSE.toml` and `LICENSES/`.
