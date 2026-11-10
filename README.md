# glimmer-qt

> Originally extracted from [Snitt](https://github.com/isaksky/snitt).

A Qt Quick backend for Glimmer on Jolt. See [EXAMPLES.md](EXAMPLES.md) to get started.

Build the native bridge with `jolt native` (or `./build-native`). Run tests with:

```sh
jolt test                # all automated suites
jolt test lifecycle      # one suite
jolt test --list         # available suites
```

`./bin/test` provides the same interface and handles building, Qt setup,
fixtures, and process deadlines.
See [TESTING.md](TESTING.md) for dependencies and desktop checks.

To use glimmer-qt from another project, add it as a git dep:

```clojure
{:deps {jolt-lang/glimmer-qt {:git/url "https://github.com/jolt-lang/glimmer-qt"
                              :git/sha "..."}}}
```

The first run builds the native bridge in the dependency's checkout (needs
jolt 0.8.26+ for `:build` in `:jolt/native`, plus Qt 6, a C++17 compiler and
`make`). The app still has to run with the Qt plugin and QML import paths of
the same SDK; `bin/qt.py` shows how the examples set them up.
