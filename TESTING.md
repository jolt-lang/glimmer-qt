# Testing

Run commands from the repository root:

```sh
jolt test                # same as ./bin/test all
jolt test --list
jolt test objects        # same as ./bin/test objects
```

The runner requires Python 3, Jolt 0.8.22 or newer, a C++17 compiler, `make`,
and Qt 6 with Quick, Quick Controls, Multimedia, and Test. The `media` suite
also requires an `ffmpeg` executable with libx264 and a Qt SDK containing the
FFmpeg multimedia plugin; see [MEDIA.md](MEDIA.md).

`all` runs the 15 Qt suites, each in its own process. It builds the production
bridge for lifecycle, scheduling, layout, and media, then builds with native
test fixtures for the remaining Qt suites.
Each configuration is built once. Tests run sequentially, continue after a
suite failure, and finish with a summary and a nonzero exit status if any fail.
A build failure stops the run immediately. Missing media dependencies are
reported as failures rather than silently skipping coverage.

Every suite has an external process deadline: normally 60 seconds, or 45
seconds for tray/services. This also bounds native event-loop and GC deadlocks.
Temporary image, video, and service fixtures are removed when the suite exits,
including on failure or timeout. Layout-loop warnings fail the layout suite.

The existing `jolt -M:…-test` aliases remain available for a manually prepared
environment. In particular, `jolt -M:test` runs only lifecycle checks; use
`jolt test` for the complete automated set.

## Configuration

- `JOLT` selects the executable launched by the scripts.
- `GLIMMER_QT_QMAKE` selects a Qt 6 SDK.
- `GLIMMER_QT_BUILD_JOBS` overrides the detected compiler parallelism.
- `QT_QPA_PLATFORM` overrides the default `offscreen` platform. Services always
  uses its isolated offscreen clipboard.

The example and test launchers share Qt setup so plugins and QML imports come
from the SDK that built the bridge. Media additionally accepts `QT_PLUGIN_PATH`
for a compatible external multimedia plugin.

## Desktop and screenshot checks

`all` excludes the macOS activation check and the interactive native chooser.
They require a desktop session and are selected explicitly:

```sh
./bin/test macos-activation
./bin/test dialogs --native-desktop
QT_QPA_PLATFORM=cocoa ./bin/test windows
GLIMMER_QT_TRAY_DESKTOP_CHECK=1 QT_QPA_PLATFORM=cocoa ./bin/test tray
```

Activation runs with both values of `QT_MAC_SET_RAISE_PROCESS`, each bounded
by 30 seconds. Native dialogs gets a temporary macOS application bundle and a
240-second deadline; select the fixture image, then cancel the second chooser.
Both commands select the Cocoa platform automatically.

Actions and controls accept an optional screenshot path, resolved from the
repository root:

```sh
./bin/test actions /tmp/actions.png
./bin/test controls /tmp/controls.png
QT_QPA_PLATFORM=cocoa GLIMMER_QT_MEDIA_SCREENSHOT=/tmp/media.png ./bin/test media
```

The launchers use the macOS/Linux build workflow. For a manual Windows build
and direct Jolt aliases, see [LIFECYCLE.md](LIFECYCLE.md#verification).

Runner failure-path checks need only Python: `python3 -m unittest discover -s test`.
