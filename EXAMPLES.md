# Examples

Run these commands from the repository root:

```sh
jolt counter
jolt hello
```

Both commands build the native bridge, select the Qt plugins and QML imports
from the same SDK, and open a window. Close the window to exit. They require
Python 3, Jolt 0.8.16 or newer, a C++17 compiler, `make`, and Qt 6 with Quick, Quick Controls,
and Multimedia; see [build requirements](LIFECYCLE.md#verification). These demos
do not use media playback or require an FFmpeg executable.

The build discovers `qmake6` or `qmake` on `PATH`, then Homebrew's Qt installation.
Set `GLIMMER_QT_QMAKE` to select another Qt SDK:

```sh
GLIMMER_QT_QMAKE=/path/to/qt/bin/qmake6 jolt hello
```

The launcher can also be called directly as `./bin/run-example hello` or
`./bin/run-example counter`. Set `JOLT` to select the Jolt executable it launches.
The launcher follows the repository's macOS/Linux build workflow; Windows
users should follow the [manual build and SDK setup](LIFECYCLE.md#verification),
then use `jolt -M:counter` or `jolt -M:hello`. Direct aliases assume the bridge is
already built and the matching Qt environment is configured.

## Counter

[counter.clj](examples/glimmer_qt/counter.clj) adapts Glimmer GTK's introductory
counter to Qt. Its Form-2 component creates a reactive atom once and returns a
render function. Increment, decrement, and reset update the label in place.

## Hello

[hello.clj](examples/glimmer_qt/hello.clj) adapts Snitt's small greeting example.
Edit the name and press Enter or **Say hello**. It demonstrates controlled text
input, reactive labels, event handlers, and expanding layouts. Blank names fall
back to "world". Its `defonce` state also supports keeping state during live
development.

Examples follow Glimmer GTK's `examples/<backend_namespace>/` convention and use
root `deps.edn` aliases with `:extra-paths ["examples"]`. They are not added to
the library's default source paths. The Qt-specific launcher is shared by both
tasks. Binary packaging is a separate step; see [the release notice](THIRD_PARTY.md).
