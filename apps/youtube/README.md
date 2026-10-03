# YouTube

Tested on YouTube 21.37.42, Android 16. Other versions and server-side layout variants are untested.

## Constraints

- Match from indexed strings, resource IDs, platform calls or retained class names. No whole-DEX scans, hard-coded resource IDs, register numbers or experiment flags.
- Playback time hooks the progress broadcaster's event constructor. The VideoTimes snapshot is not updated by every provider.
- Player mode hooks both the legacy overlay and the native player-view setter. The newer provider bypasses the overlay.
- Litho parsers are observed without changing their model. Routing UPB through FlatBuffers breaks expandable posts.
- Return YouTube Dislike (RYD) never blocks rendering on the network. Likes come from YouTube's accessibility count, never RYD estimates. Pending or failed requests show a dash.
- Swipe controls wrap the activity's touch dispatch without replacing its superclass. Open engagement panels keep their scrolling. Brightness is window-local.
- The fullscreen button is aligned to the adjacent player icon after layout. YouTube owns its size and appearance.
- Thumbnail verification stays off the UI thread. Third-party thumbnail requests omit YouTube tracking parameters.
- Shorts without a dislike button get no replacement control.
- Every branding alias, custom ones included, is in every build. Android keeps an alias's enabled state across updates.
