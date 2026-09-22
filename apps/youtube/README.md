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
  action bar uses matching native Litho labels beneath its existing like/dislike icons,
  inside their original clickable buttons. Likes come from YouTube's native accessibility
  count, never RYD estimates. Fetch/vote/settings events update labels without rebinding.
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

Build with `./gradlew bundle`, patch the supported APK, and check the result on-device.
The current device validation target is YouTube **21.37.42** on Android 16; other versions
and server-side layout variants are not guaranteed by these checks.

### Compact action bar

The device received `compactify_video_action_bar` with icon-only like/dislike buttons.
The patch retains the existing gesture component and its voting handlers, placing a
small secondary-color count below each icon within its original horizontal footprint.
Both icons share one row and both labels share one baseline. The existing indexed
Litho filter observes YouTube's like-button accessibility count; absent or unparseable
counts remain unavailable instead of falling back to estimates.
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
