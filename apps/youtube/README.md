# YouTube feature migration

The SponsorBlock, Return YouTube Dislike, alternative thumbnails and swipe-controls
patches use the shared Reseam settings, player, video, controls and Litho hooks.
Branding and theme build on the existing Reseam patches rather than introducing a
second implementation.

## Matching and shared hooks

- Start from indexed strings, resource IDs, platform calls or retained class names;
  follow typed fields and callees inside the resulting class. No whole-DEX custom
  scan, hard-coded resource ID, register number or experiment flag is required.
- The thumbnail constructor is shared with region-restriction bypass. Both image
  response-parser variants are hooked through Cronet's stable callback signatures.
- RYD follows TextComponent's common formatter, covering TextViewComponent too.
  Span replacement is idempotent when both entry points process the same text.
- Playback time follows the event constructor used by the real progress broadcaster.
  The old VideoTimes UI snapshot is not updated by every server-selected provider.
  Duration comes from the event's playback-window bounds.
- Player mode observes both the legacy overlay and native player-view setter. The
  newer provider can bypass the legacy overlay entirely.
- Both Litho parsers are observed without changing their model representation.
  Routing UPB through FlatBuffers breaks expandable posts that serialize to protobuf.

## Runtime design

- RYD never waits for a network response in rendering or player-response hooks. The compact
  action bar uses a native Litho label beneath its existing dislike icon, inside the same
  clickable button. Fetch/vote/settings events update it without rebinding the component.
  Pending/failed requests show a dash, never another video's count. Legacy inline text,
  including existing Shorts dislike labels, still updates on binds. There is no player overlay.
  Text matching reads the resolved path directly without serializing the conversion
  context. Every API request disconnects on success, failure, and exceptions.
- SponsorBlock has immutable segment results, a bounded executor/cache, stale-video
  request protection, overlap-aware skipping and duration calculation. Controls use
  the actual player bounds and receive visibility changes even while paused. Votes
  and submissions use native dialogs; submission requires explicit confirmation.
- Swipe controls intercept only the activity's touch/key dispatch. They do not replace
  its superclass. Vertical gestures cancel the app's gesture at touch slop; the user's
  threshold controls when brightness/volume adjustment begins. Horizontal, multi-touch
  and excluded-area gestures remain with YouTube. Brightness is window-local.
- Thumbnail verification stays off the UI thread, including avoiding a monitor held
  by an in-flight verification. Fast mode uses response callbacks for missing images.
  Third-party thumbnail requests omit YouTube tracking parameters.
- Branding enables the new launcher alias before disabling the previous one. Theme
  resources cover the newer baseline palette and use day/night splash selectors.

## Verification

Build the bundle and run the network-independent interval regressions:

```sh
./gradlew :apps:youtube:extensions:sponsorblock:testTimeline bundle
```

The ten regression checks cover transitive overlap, touching intervals, gaps,
eligibility, clipping, empty lists and zero-duration highlights.

After installing the current combined APK, run the isolated Android RYD regressions
from this repository root (requires JDK 17 and `ANDROID_HOME`):

```sh
bash apps/youtube/extensions/dislike/src/test/run-device-tests.sh
```

On rooted devices whose system font configuration is unreadable to the shell (including
this Android 16 device), use `RYD_TEST_ROOT=1` to initialize Android's fonts in the isolated
test process. This does not grant the harness access to app preferences or real network votes.

The harness loads the installed APK's actual extension classes in a separate Android
process, uses in-memory settings, and intercepts every URL connection. It does not
read/write the app's preferences or publish votes. The suite contains 171 assertions.
Coverage includes pending/failed fetches, cached counts,
repeated binds, styling, rolling/static geometry, live appearance toggles, Shorts video
association, stale responses, cache expiration, 800 concurrent render/vote updates per
run, and all five API endpoints' success/error/timeout/disconnection paths. Malformed,
negative and oversized responses and fetch rate-limit backoff are also covered. Native
label tests cover regular/Shorts video association, disabled settings, actual drawable
rendering, percentage formatting, accessibility text, and fetch/vote notifications.

The expanded tests caught cached static spans being reused for rolling-number text.
The cache now includes layout and appearance settings, so separator geometry and
percentage/compact/hidden-like changes cannot reuse an incompatible cached span.

Tested against the declared YouTube **21.37.42** APK on the connected Android 16
device. Separate patch runs passed for SponsorBlock, RYD, thumbnails, swipe controls,
branding + theme, and thumbnails + region-restriction bypass. Each run produced a
signed APK in approximately 3 seconds on the development machine; this is total
patching/output time, not fingerprint-only time or a cross-machine benchmark.

Combined-device checks covered dislike rendering without duplicate counts, real
SponsorBlock segment retrieval, automatic skipping, jump-to-start, adjusted duration,
seekbar markers, native menus, still-thumbnail replacement, launcher alias switching,
fullscreen volume/brightness overlays, and expandable posts without the Litho crash.
The combined APK also included GmsCore support, the settings entry, ad patches and
the existing stream-spoofing patch for playback compatibility.

The subsequent RYD regression run also opened and expanded a long image post via
its “Read more” control on the channel's Posts tab. No protobuf serialization crash
or app-process error was recorded. The six isolated patch runs were repeated after
the RYD cache correction and all passed (approximately 3.0–3.1 seconds each).

SponsorBlock voting/submission publication is deliberately not part of the device
smoke test. Service timeout behavior was encountered as well as successful responses.
Other APK versions, Android versions, locales and all server-side experiment variants
have not been exhaustively validated; compatibility declarations were not broadened.

### Compact action bar

The device received `compactify_video_action_bar` with icon-only like/dislike buttons.
The patch retains the existing gesture component and its voting handlers, placing a
small secondary-color count below the icon within its original horizontal footprint.
Height and alignment properties are resolved through retained Yoga bridge calls and
typed native builders, not obfuscated field names or experiment IDs. Litho owns the
label's layout, drawable callbacks and lifecycle. Process-lifetime callbacks retain only
weak label references; refreshes do not poll or scan the view tree. Accessibility exposes
the live count, and settings changes invalidate the mounted label.

The current Shorts experiment omits the dislike button entirely. This migration does
not invent a replacement voting control or place a floating count over the video.
Shorts variants with existing dislike text continue to use the inline text hooks.
Real public votes are not part of the device checks. Large-font/RTL, multi-window and
other YouTube/Android versions still need broader device coverage.
