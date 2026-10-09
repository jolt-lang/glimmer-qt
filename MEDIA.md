# Qt review playback

Before distributing a binary with media support, follow the
[binary release notice](THIRD_PARTY.md#developer-notice-before-distributing-binaries),
including the checks for the selected Qt and FFmpeg runtime.

Require `glimmer-qt.media` before mounting a `:video-player`. The tag is a visual
Qt Quick surface with an owned `MediaPlayer`, `AudioOutput`, `VideoOutput`, and
`QVideoSink`. It accepts `:id`, `:hexpand`, `:vexpand`, `:fill-mode` (`"fit"`,
`"crop"`, or `"stretch"`), and `:on-first-frame`. The callback takes no frame
data and fires once for the first valid decoded frame of each nonempty source.
Clearing and loading another source rearms it.

Use `glimmer-qt.media/player`, `audio-output`, `video-output`, or `video-sink` to
borrow typed handles for inspection. The surface owns them; unmounting it
invalidates every handle and disconnects its observers. Playback mutations run
on the Qt GUI thread through `source!`, `position!`, `rate!`, `mute!`, `play!`,
`pause!`, `stop!`, and `clear!`. `clear!` stops and clears the source; call it
before trimming or replacing the current file. The media controls are
imperative so an unrelated Glimmer render cannot reset a seek, rate, or source.

`state` returns `:source`, `:position`, `:duration`, `:playback`, `:media`,
`:error`, `:error-string`, `:rate`, and `:muted`. Enum fields are typed
`glimmer-qt.value/enum` values, such as `(value/enum "PlayingState")`.
`observe!` delivers typed source, position, duration, playback, media, and error
changes through the owned QObject subscription path. Read initial state with
`state`; notifications report subsequent changes. For muted priming, mute,
start playback, then pause and seek in `:on-first-frame` before unmuting. A
short fallback timer can finish priming if the backend does not produce a frame.

`./test-media` uses `ffmpeg` on `PATH` to generate a static-frame MP4 and
loads the selected Qt SDK's FFmpeg multimedia backend. The SDK must include
that plugin; the standalone `ffmpeg` command alone is insufficient. Use
`GLIMMER_QT_QMAKE` to select an SDK containing it, or `QT_PLUGIN_PATH` for another
compatible plugin installation. `QT_MEDIA_BACKEND` can select a different
backend for investigation; the macOS native backend stalled while loading the
test clip during extraction. The suite checks playback, seek, rate, first-frame
delivery, typed states and errors, source replacement, and teardown. The
default offscreen run checks decoding and sink delivery using software decoding
via [Qt's FFmpeg configuration](https://doc.qt.io/qt-6/advanced-ffmpeg-configuration.html).
Run with
`QT_QPA_PLATFORM=cocoa SNITT_MEDIA_SCREENSHOT=/tmp/media.png` to capture the
window on the macOS desktop.
